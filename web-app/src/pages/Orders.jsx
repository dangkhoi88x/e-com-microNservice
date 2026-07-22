import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Pagination,
  Paper,
  Select,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from "@mui/material";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import AdminPanelSettingsOutlinedIcon from "@mui/icons-material/AdminPanelSettingsOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { useEffect, useState } from "react";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { hasAnyRole } from "../services/authenticationService";
import { confirmInventory } from "../services/inventoryService";
import {
  cancelOrder,
  getAllOrders,
  getMyOrders,
  updateOrderStatus,
} from "../services/orderService";
import { formatDateTime } from "../utils/dateTimeUtils";

const cancellableStatuses = new Set(["PENDING", "CONFIRMED"]);
const orderStatuses = [
  "ALL",
  "PENDING",
  "PENDING_PAYMENT",
  "INVENTORY_FAILED",
  "CONFIRMED",
  "SHIPPING",
  "COMPLETED",
  "CANCELLED",
];
const adminStatusActions = ["CONFIRMED", "SHIPPING", "COMPLETED"];
const orderStatusSteps = ["PENDING", "PENDING_PAYMENT", "CONFIRMED", "SHIPPING", "COMPLETED"];
const completableStatuses = new Set(["PENDING_PAYMENT", "CONFIRMED", "SHIPPING"]);

const statusColor = (status) => {
  switch (status) {
    case "PENDING":
    case "PENDING_PAYMENT":
      return "warning";
    case "INVENTORY_FAILED":
      return "error";
    case "CONFIRMED":
      return "info";
    case "SHIPPING":
      return "primary";
    case "COMPLETED":
      return "success";
    case "CANCELLED":
      return "default";
    default:
      return "default";
  }
};

const formatPrice = (value) => {
  if (value === null || value === undefined) return "-";

  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value));
};

const getCompletedSteps = (status) => {
  const index = orderStatusSteps.indexOf(status);
  if (index < 0) return 0;
  return index + 1;
};

export default function Orders() {
  const isAdmin = hasAnyRole("ROLE_ADMIN", "ADMIN");
  const [orders, setOrders] = useState([]);
  const [viewMode, setViewMode] = useState("mine");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [pageInfo, setPageInfo] = useState({
    currentPage: 1,
    pageSize: 10,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const visibleOrders =
    statusFilter === "ALL"
      ? orders
      : orders.filter((order) => order.status === statusFilter);

  const loadOrders = async (page = pageInfo.currentPage, mode = viewMode) => {
    setLoading(true);
    setErrorMessage("");

    try {
      const request = mode === "all" && isAdmin ? getAllOrders : getMyOrders;
      const data = await request({
        page,
        size: pageInfo.pageSize,
      });

      setOrders(data.content || []);
      setPageInfo({
        currentPage: data.currentPage || page,
        pageSize: data.pageSize || pageInfo.pageSize,
        totalPages: data.totalPages || 0,
        totalElements: data.totalElements || 0,
      });
    } catch (error) {
      setErrorMessage(error.response?.data?.message || "Could not load orders.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders(1, viewMode);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewMode]);

  const handleViewDetail = (order) => {
    setDetailLoading(false);
    setErrorMessage("");
    setSelectedOrder(order);
  };

  const handleCancelOrder = async (order) => {
    if (!window.confirm(`Cancel order ${order.orderCode || order.id}?`)) return;

    setErrorMessage("");
    setSuccessMessage("");

    try {
      const updatedOrder = await cancelOrder(order.id);
      setSuccessMessage("Order cancelled successfully.");
      setOrders((current) =>
        current.map((item) => (item.id === updatedOrder.id ? updatedOrder : item)),
      );
      if (selectedOrder?.id === updatedOrder.id) {
        setSelectedOrder(updatedOrder);
      }
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not cancel order.",
      );
    }
  };

  const handleUpdateStatus = async (order, status) => {
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const updatedOrder = await updateOrderStatus(order.id, status);
      setSuccessMessage(`Order status updated to ${status}.`);
      setOrders((current) =>
        current.map((item) => (item.id === updatedOrder.id ? updatedOrder : item)),
      );
      if (selectedOrder?.id === updatedOrder.id) {
        setSelectedOrder(updatedOrder);
      }
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not update order status.",
      );
    }
  };

  const handleCompleteOrder = async (order) => {
    if (!window.confirm(`Mark order ${order.orderCode || order.id} as completed?`)) return;

    setErrorMessage("");
    setSuccessMessage("");

    try {
      await confirmInventory(order.id);
      await handleUpdateStatus(order, "COMPLETED");
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message ||
          "Could not confirm inventory for this order.",
      );
    }
  };

  const handleViewModeChange = () => {
    setViewMode((current) => (current === "mine" ? "all" : "mine"));
    setSelectedOrder(null);
    setStatusFilter("ALL");
  };

  const renderStatus = (status) => (
    <Chip
      size="small"
      label={status || "UNKNOWN"}
      color={statusColor(status)}
      variant="outlined"
    />
  );

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Fulfillment"
        title="Orders"
        description="Track orders, inspect items, and manage eligible order actions."
        actions={
          <>
          {isAdmin && (
            <Button
              variant={viewMode === "all" ? "contained" : "outlined"}
              startIcon={<AdminPanelSettingsOutlinedIcon />}
              onClick={handleViewModeChange}
            >
              {viewMode === "all" ? "My orders" : "All orders"}
            </Button>
          )}
          <Button
            variant="outlined"
            startIcon={<RefreshOutlinedIcon />}
            onClick={() => loadOrders(pageInfo.currentPage)}
          >
            Refresh
          </Button>
          </>
        }
      />

      <Paper
        className="admin-data-panel"
        elevation={0}
        sx={{
          mt: 3,
          border: "1px solid",
          borderColor: "divider",
          overflow: "hidden",
        }}
      >
        <Tabs
          value={statusFilter}
          onChange={(_, value) => setStatusFilter(value)}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ px: 2, borderBottom: "1px solid", borderColor: "divider" }}
        >
          {orderStatuses.map((status) => (
            <Tab
              key={status}
              value={status}
              label={status === "ALL" ? "All" : status}
            />
          ))}
        </Tabs>

        <Stack
          direction={{ xs: "column", sm: "row" }}
          justifyContent="space-between"
          alignItems={{ xs: "stretch", sm: "center" }}
          spacing={2}
          className="panel-summary"
        >
          <Stack direction="row" spacing={1.25} alignItems="center" className="section-library-heading">
            <Box className="section-heading-icon section-heading-icon-violet"><ReceiptLongOutlinedIcon /></Box>
            <Box>
            <Typography className="section-heading-title">
              {viewMode === "all" ? "All order activity" : "My order activity"}
            </Typography>
            <Typography className="section-heading-description">
              {pageInfo.totalElements} orders tracked across fulfillment states
            </Typography>
            </Box>
          </Stack>
          <Chip
            label={`${visibleOrders.length} visible`}
            color="primary"
            variant="outlined"
          />
        </Stack>

        {errorMessage && (
          <Alert severity="error" sx={{ m: 2 }}>
            {errorMessage}
          </Alert>
        )}
        {successMessage && (
          <Alert severity="success" sx={{ m: 2 }} onClose={() => setSuccessMessage("")}>
            {successMessage}
          </Alert>
        )}

        {loading ? (
          <Stack alignItems="center" justifyContent="center" sx={{ py: 8 }}>
            <CircularProgress />
          </Stack>
        ) : visibleOrders.length === 0 ? (
          <Box sx={{ p: 4 }}>
            <Typography fontWeight={800}>No orders found</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Change status filter or create an order from the API.
            </Typography>
          </Box>
        ) : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Order</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Total</TableCell>
                    <TableCell>Created At</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {visibleOrders.map((order) => (
                    <TableRow key={order.id} hover>
                      <TableCell>
                        <Stack direction="row" spacing={1.5} alignItems="center">
                          <Box className="order-mark">
                            <ReceiptLongOutlinedIcon fontSize="small" />
                          </Box>
                          <Box sx={{ minWidth: 0 }}>
                            <Typography className="table-primary" noWrap>
                              {order.orderCode || order.id}
                            </Typography>
                            <Typography className="table-secondary" noWrap>
                              {viewMode === "all" ? order.userId : order.shippingAddress || "No shipping address"}
                            </Typography>
                          </Box>
                        </Stack>
                      </TableCell>
                      <TableCell>{renderStatus(order.status)}</TableCell>
                      <TableCell align="right">
                        <Typography className="money-value">
                          {formatPrice(order.totalAmount)}
                        </Typography>
                      </TableCell>
                      <TableCell>{formatDateTime(order.createdAt)}</TableCell>
                      <TableCell align="right">
                        <Box className="row-actions">
                          <Tooltip title="View detail">
                            <IconButton onClick={() => handleViewDetail(order)}>
                              <VisibilityOutlinedIcon />
                            </IconButton>
                          </Tooltip>
                          {completableStatuses.has(order.status) && (
                            <Tooltip title="Complete order">
                              <IconButton
                                color="success"
                                onClick={() => handleCompleteOrder(order)}
                              >
                                <CheckCircleOutlineOutlinedIcon />
                              </IconButton>
                            </Tooltip>
                          )}
                        {isAdmin && viewMode === "all" && (
                          <Select
                            size="small"
                            displayEmpty
                            value=""
                            onChange={(event) =>
                              handleUpdateStatus(order, event.target.value)
                            }
                            sx={{ ml: 1, minWidth: 132 }}
                          >
                            <MenuItem value="" disabled>
                              Update
                            </MenuItem>
                            {adminStatusActions.map((status) => (
                              <MenuItem
                                key={status}
                                value={status}
                                disabled={order.status === status}
                              >
                                {status}
                              </MenuItem>
                            ))}
                          </Select>
                        )}
                        {viewMode === "mine" && cancellableStatuses.has(order.status) && (
                          <Tooltip title="Cancel order">
                            <IconButton
                              color="error"
                              onClick={() => handleCancelOrder(order)}
                            >
                              <CancelOutlinedIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <Stack
              direction={{ xs: "column", sm: "row" }}
              alignItems="center"
              justifyContent="space-between"
              spacing={2}
              sx={{ px: 2, py: 2, borderTop: "1px solid", borderColor: "divider" }}
            >
              <Typography variant="body2" color="text.secondary">
                {pageInfo.totalElements} orders
                {statusFilter !== "ALL" ? `, ${visibleOrders.length} shown` : ""}
              </Typography>
              <Pagination
                page={pageInfo.currentPage}
                count={Math.max(pageInfo.totalPages, 1)}
                color="primary"
                onChange={(_, page) => loadOrders(page)}
              />
            </Stack>
          </>
        )}
      </Paper>

      <Dialog
        open={Boolean(selectedOrder) || detailLoading}
        onClose={() => setSelectedOrder(null)}
        fullWidth
        maxWidth="md"
      >
        <DialogTitle>
          {detailLoading ? "Loading order..." : "Order detail"}
        </DialogTitle>
        <DialogContent dividers>
          {detailLoading ? (
            <Stack alignItems="center" sx={{ py: 6 }}>
              <CircularProgress />
            </Stack>
          ) : selectedOrder ? (
            <Stack spacing={3}>
              <Box className="detail-hero">
                <Stack
                  direction={{ xs: "column", sm: "row" }}
                  justifyContent="space-between"
                  alignItems={{ xs: "stretch", sm: "center" }}
                  spacing={2}
                >
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      Fulfillment progress
                    </Typography>
                    <Typography fontWeight={900}>
                      {selectedOrder.status || "UNKNOWN"}
                    </Typography>
                  </Box>
                  <LocalShippingOutlinedIcon className="detail-hero-icon" />
                </Stack>
                <Box className="order-progress">
                  <Box
                    sx={{
                      width: `${Math.min(
                        100,
                        (getCompletedSteps(selectedOrder.status) /
                          orderStatusSteps.length) *
                          100,
                      )}%`,
                    }}
                  />
                </Box>
              </Box>
              <Stack
                direction={{ xs: "column", sm: "row" }}
                justifyContent="space-between"
                spacing={2}
              >
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Order code
                  </Typography>
                  <Typography fontWeight={900}>{selectedOrder.orderCode || selectedOrder.id}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    Internal ID: {selectedOrder.id}
                  </Typography>
                </Box>
                <Box>{renderStatus(selectedOrder.status)}</Box>
              </Stack>

              <Stack direction={{ xs: "column", sm: "row" }} spacing={3}>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Total
                  </Typography>
                  <Typography fontWeight={900}>
                    {formatPrice(selectedOrder.totalAmount)}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    User
                  </Typography>
                  <Typography fontWeight={800}>
                    {selectedOrder.userId || "-"}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Created At
                  </Typography>
                  <Typography fontWeight={800}>
                    {formatDateTime(selectedOrder.createdAt)}
                  </Typography>
                </Box>
              </Stack>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Shipping Address
                </Typography>
                <Typography fontWeight={800}>
                  {selectedOrder.shippingAddress || "-"}
                </Typography>
              </Box>

              <Paper
                variant="outlined"
                sx={{
                  p: 2,
                  backgroundColor: selectedOrder.promotionCode ? "success.50" : "background.paper",
                  borderColor: selectedOrder.promotionCode ? "success.200" : "divider",
                }}
              >
                <Typography variant="subtitle2" fontWeight={900} gutterBottom>
                  Promotion and price breakdown
                </Typography>
                <Stack spacing={0.75}>
                  <Stack direction="row" justifyContent="space-between" spacing={2}>
                    <Typography variant="body2" color="text.secondary">Promotion code</Typography>
                    <Typography variant="body2" fontWeight={800}>
                      {selectedOrder.promotionCode || "Not applied"}
                    </Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between" spacing={2}>
                    <Typography variant="body2" color="text.secondary">Subtotal</Typography>
                    <Typography variant="body2" fontWeight={800}>{formatPrice(selectedOrder.subtotalAmount)}</Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between" spacing={2}>
                    <Typography variant="body2" color="text.secondary">Promotion discount</Typography>
                    <Typography variant="body2" fontWeight={900} color={Number(selectedOrder.discountAmount || 0) > 0 ? "success.main" : "text.primary"}>
                      {Number(selectedOrder.discountAmount || 0) > 0 ? `-${formatPrice(selectedOrder.discountAmount)}` : formatPrice(0)}
                    </Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between" spacing={2} sx={{ pt: 0.75, borderTop: "1px solid", borderColor: "divider" }}>
                    <Typography variant="body2" fontWeight={900}>Total paid</Typography>
                    <Typography variant="body2" fontWeight={900}>{formatPrice(selectedOrder.totalAmount)}</Typography>
                  </Stack>
                </Stack>
              </Paper>

              <TableContainer component={Paper} elevation={0} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Product</TableCell>
                      <TableCell align="right">Price</TableCell>
                      <TableCell align="right">Quantity</TableCell>
                      <TableCell align="right">Subtotal</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(selectedOrder.items || []).map((item) => (
                      <TableRow key={item.productId}>
                        <TableCell>
                          <Typography fontWeight={800}>
                            {item.productName}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {item.productId}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          {formatPrice(item.price)}
                        </TableCell>
                        <TableCell align="right">{item.quantity}</TableCell>
                        <TableCell align="right">
                          {formatPrice(item.subtotal)}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Stack>
          ) : null}
        </DialogContent>
        <DialogActions>
          {selectedOrder && viewMode === "mine" && cancellableStatuses.has(selectedOrder.status) && (
            <Button
              color="error"
              startIcon={<CancelOutlinedIcon />}
              onClick={() => handleCancelOrder(selectedOrder)}
            >
              Cancel order
            </Button>
          )}
          {selectedOrder && isAdmin && viewMode === "all" && (
            <Select
              size="small"
              displayEmpty
              value=""
              onChange={(event) =>
                handleUpdateStatus(selectedOrder, event.target.value)
              }
              sx={{ minWidth: 150 }}
            >
              <MenuItem value="" disabled>
                Update status
              </MenuItem>
              {adminStatusActions.map((status) => (
                <MenuItem
                  key={status}
                  value={status}
                  disabled={selectedOrder.status === status}
                >
                  {status}
                </MenuItem>
              ))}
            </Select>
          )}
          <Button onClick={() => setSelectedOrder(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </MainLayout>
  );
}
