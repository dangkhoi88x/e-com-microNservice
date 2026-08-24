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
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import DoneOutlinedIcon from "@mui/icons-material/DoneOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import LocationOnOutlinedIcon from "@mui/icons-material/LocationOnOutlined";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { hasAdminRole } from "../services/authenticationService";
import { confirmInventory } from "../services/inventoryService";
import {
  cancelOrder,
  getAllOrders,
  getMyOrders,
  searchAdminOrder,
  updateOrderStatus,
} from "../services/orderService";
import { formatDateTime } from "../utils/dateTimeUtils";
import "./Orders.css";

const cancellableStatuses = new Set(["PENDING", "CONFIRMED"]);
const orderStatuses = [
  "ALL",
  "PENDING",
  "PENDING_PAYMENT",
  "INVENTORY_FAILED",
  "PROMOTION_FAILED",
  "CONFIRMED",
  "SHIPPING",
  "DELIVERY_FAILED",
  "RETURNING",
  "RETURNED",
  "COMPLETED",
  "CANCELLED",
];
const adminStatusActions = ["CONFIRMED", "SHIPPING", "COMPLETED"];
const completableStatuses = new Set(["PENDING_PAYMENT", "CONFIRMED", "SHIPPING"]);

const statusColor = (status) => {
  switch (status) {
    case "PENDING":
    case "PENDING_PAYMENT":
      return "warning";
    case "INVENTORY_FAILED":
    case "PROMOTION_FAILED":
      return "error";
    case "CONFIRMED":
      return "info";
    case "SHIPPING":
      return "primary";
    case "DELIVERY_FAILED":
      return "error";
    case "RETURNING":
    case "RETURNED":
      return "warning";
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

const fulfillmentSteps = [
  { status: "PENDING", label: "Đã đặt" },
  { status: "CONFIRMED", label: "Xác nhận" },
  { status: "SHIPPING", label: "Đang giao" },
  { status: "COMPLETED", label: "Hoàn tất" },
];

const getFulfillmentStep = (status) => {
  if (["PENDING", "PENDING_PAYMENT", "INVENTORY_FAILED", "PROMOTION_FAILED"].includes(status)) return 0;
  if (status === "CONFIRMED") return 1;
  if (status === "SHIPPING") return 2;
  if (status === "COMPLETED") return 3;
  return -1;
};

const statusLabel = (status) => ({
  PROMOTION_FAILED: "Không thể giữ ưu đãi",
  PENDING: "Chờ xử lý",
  PENDING_PAYMENT: "Chờ thanh toán",
  INVENTORY_FAILED: "Không đủ tồn kho",
  CONFIRMED: "Đã xác nhận",
  SHIPPING: "Đang giao hàng",
  DELIVERY_FAILED: "Giao hàng thất bại",
  RETURNING: "Đang hoàn hàng",
  RETURNED: "Đã hoàn hàng",
  COMPLETED: "Hoàn tất",
  CANCELLED: "Đã hủy",
}[status] || status || "Không xác định");

export default function Orders() {
  const [searchParams] = useSearchParams();
  const isAdmin = hasAdminRole();
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

  const orderQuery = (searchParams.get("query") || "").trim().toLowerCase();
  const statusOrders =
    statusFilter === "ALL"
      ? orders
      : orders.filter((order) => order.status === statusFilter);
  const visibleOrders = !orderQuery ? statusOrders : statusOrders.filter((order) => [order.orderCode, order.id, order.userId].some((value) => value?.toLowerCase().includes(orderQuery)));

  const loadOrders = async (page = pageInfo.currentPage, mode = viewMode) => {
    setLoading(true);
    setErrorMessage("");

    try {
      if (orderQuery && isAdmin) {
        const order = await searchAdminOrder(orderQuery);
        setOrders(order ? [order] : []);
        setPageInfo((current) => ({ ...current, currentPage: 1, totalPages: 1, totalElements: order ? 1 : 0 }));
        return;
      }
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
  }, [viewMode, orderQuery]);

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
        PaperProps={{ className: "order-detail-modal", sx: { borderRadius: 4, overflow: "hidden", maxWidth: 860, border: "1px solid", borderColor: "divider", boxShadow: "0 24px 60px -20px rgba(15,23,32,.35)" } }}
      >
        <DialogTitle sx={{ px: { xs: 2.5, md: 4 }, py: 2.5, borderBottom: "1px solid", borderColor: "divider", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Typography className="order-detail-modal-title" fontSize={20} fontWeight={800} letterSpacing="-0.3px">{detailLoading ? "Đang tải đơn hàng..." : "Chi tiết đơn hàng"}</Typography>
          <IconButton aria-label="Đóng chi tiết đơn hàng" onClick={() => setSelectedOrder(null)} sx={{ bgcolor: "grey.50", border: "1px solid", borderColor: "divider", borderRadius: 2 }}><CloseOutlinedIcon fontSize="small" /></IconButton>
        </DialogTitle>
        <DialogContent sx={{ px: { xs: 2.5, md: 4 }, py: 3.5, bgcolor: "#fcfcfd" }}>
          {detailLoading ? (
            <Stack alignItems="center" sx={{ py: 6 }}>
              <CircularProgress />
            </Stack>
          ) : selectedOrder ? (
            <Stack className="order-detail-modal-content" spacing={3.25}>
              {(() => {
                const activeStep = getFulfillmentStep(selectedOrder.status);
                const isException = activeStep < 0;
                return <Box sx={{ p: { xs: 2, md: 3 }, borderRadius: 3, border: "1px solid", borderColor: isException ? "warning.200" : "#cfeae7", background: isException ? "#fffaf0" : "linear-gradient(180deg, #e6f4f3 0%, #f4fbfa 100%)" }}>
                  <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={2} sx={{ mb: 2.5 }}>
                    <Box><Typography variant="overline" color={isException ? "warning.dark" : "#0a5f5e"} fontWeight={800} letterSpacing=".08em">TIẾN TRÌNH GIAO HÀNG</Typography><Stack direction="row" alignItems="center" spacing={1}><Box sx={{ width: 9, height: 9, borderRadius: "50%", bgcolor: isException ? "warning.main" : "#0e7c7b", boxShadow: "0 0 0 5px rgba(14,124,123,.12)" }} /><Typography fontWeight={900} fontSize={22}>{statusLabel(selectedOrder.status)}</Typography></Stack></Box>
                    <Box sx={{ textAlign: { xs: "left", sm: "right" } }}><Typography fontFamily="monospace" fontWeight={700}>{selectedOrder.orderCode || selectedOrder.id}</Typography><Chip size="small" label={selectedOrder.status || "UNKNOWN"} sx={{ mt: .75, bgcolor: isException ? "warning.main" : "#0e7c7b", color: "common.white", fontWeight: 800, letterSpacing: ".04em" }} /></Box>
                  </Stack>
                  <Stack direction="row" sx={{ overflowX: "auto", minWidth: 0 }}>
                    {fulfillmentSteps.map((step, index) => { const complete = !isException && index <= activeStep; return <Box key={step.status} sx={{ flex: 1, minWidth: 92, textAlign: "center", position: "relative", "&:not(:last-child)::after": { content: '""', position: "absolute", top: 15, left: "50%", width: "100%", height: 3, bgcolor: complete && index < activeStep ? "#0e7c7b" : "#cfe1df", zIndex: 0 } }}><Box sx={{ mx: "auto", position: "relative", zIndex: 1, width: 30, height: 30, borderRadius: "50%", display: "grid", placeItems: "center", bgcolor: complete ? "#0e7c7b" : "#d9e6e5", color: complete ? "common.white" : "text.secondary", boxShadow: complete ? "0 2px 6px rgba(14,124,123,.35)" : "none" }}>{complete ? <DoneOutlinedIcon fontSize="small" /> : index + 1}</Box><Typography mt={1} fontSize={12} fontWeight={700} color={complete ? "#0a5f5e" : "text.secondary"}>{step.label}</Typography><Typography fontSize={10.5} color="text.secondary">{index === 0 ? formatDateTime(selectedOrder.createdAt) : complete ? "Đã cập nhật" : "Chưa tới"}</Typography></Box>; })}
                  </Stack>
                </Box>;
              })()}

              <Box><Typography className="order-detail-section-title" variant="overline" fontWeight={800} color="text.secondary" letterSpacing=".08em">THÔNG TIN ĐƠN HÀNG</Typography><Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "repeat(3, 1fr)" }, gap: 1.5, mt: 1 }}>
                {[{ label: "Tổng tiền", value: formatPrice(selectedOrder.totalAmount) }, { label: "Khách hàng", value: selectedOrder.userId || "-", mono: true, icon: <PersonOutlineOutlinedIcon fontSize="small" /> }, { label: "Ngày tạo", value: formatDateTime(selectedOrder.createdAt) }, { label: "Địa chỉ giao hàng", value: selectedOrder.shippingAddress || "-", wide: true, icon: <LocationOnOutlinedIcon fontSize="small" /> }, { label: "Internal ID", value: selectedOrder.id, wide: true, mono: true }].map((field) => <Box key={field.label} sx={{ gridColumn: field.wide ? { sm: "span 3" } : undefined, p: 1.75, border: "1px solid", borderColor: "divider", borderRadius: 2.5, bgcolor: "grey.50" }}><Typography className="order-detail-label" fontSize={11.5} color="text.secondary">{field.label}</Typography><Stack direction="row" spacing={.75} alignItems="center" mt={.4}>{field.icon}<Typography className={`order-detail-value${field.mono ? " order-detail-mono" : ""}`} fontWeight={700} fontSize={field.mono ? 12 : 14} sx={{ overflowWrap: "anywhere" }}>{field.value}</Typography></Stack></Box>)}
              </Box></Box>

              <Box><Typography className="order-detail-section-title" variant="overline" fontWeight={800} color="text.secondary" letterSpacing=".08em">KHUYẾN MÃI & THANH TOÁN</Typography><Paper variant="outlined" sx={{ mt: 1, p: { xs: 2, md: 2.5 }, borderRadius: 2.5 }}><Stack spacing={1.1}>
                <Box className="order-detail-total-row" sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", width: "100%" }}><Typography className="order-detail-label" color="text.secondary">Tạm tính</Typography><Typography className="order-detail-value" fontWeight={700}>{formatPrice(selectedOrder.subtotalAmount)}</Typography></Box>
                <Box className="order-detail-total-row" sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", width: "100%" }}><Typography className="order-detail-label" color="text.secondary">Mã khuyến mãi {selectedOrder.promotionCode && <Chip icon={<LocalOfferOutlinedIcon />} label={selectedOrder.promotionCode} size="small" sx={{ ml: .75, bgcolor: "#fdf1e1", color: "#b6650a", fontFamily: "monospace", fontWeight: 700 }} />}</Typography><Typography className="order-detail-value" fontWeight={800} color={Number(selectedOrder.discountAmount || 0) > 0 ? "warning.dark" : "text.primary"}>{Number(selectedOrder.discountAmount || 0) > 0 ? `-${formatPrice(selectedOrder.discountAmount)}` : formatPrice(0)}</Typography></Box>
                <Box className="order-detail-total-row order-detail-grand-total" sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", width: "100%", pt: 1.5, mt: .4, borderTop: "1px solid", borderColor: "divider" }}><Typography className="order-detail-value" fontWeight={900}>Tổng thanh toán</Typography><Typography className="order-detail-grand-amount" fontWeight={900} fontSize={22} color="#0a5f5e">{formatPrice(selectedOrder.totalAmount)}</Typography></Box>
              </Stack></Paper></Box>

              <Box><Typography className="order-detail-section-title" variant="overline" fontWeight={800} color="text.secondary" letterSpacing=".08em">SẢN PHẨM</Typography><TableContainer component={Paper} elevation={0} variant="outlined" sx={{ mt: 1, borderRadius: 2.5 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Sản phẩm</TableCell>
                      <TableCell align="right">Đơn giá</TableCell>
                      <TableCell align="right">SL</TableCell>
                      <TableCell align="right">Thành tiền</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(selectedOrder.items || []).map((item) => (
                      <TableRow key={item.productId}>
                        <TableCell>
                          <Stack direction="row" spacing={1.25} alignItems="center"><Box sx={{ width: 40, height: 40, display: "grid", placeItems: "center", borderRadius: 1.5, bgcolor: "#dceeed", color: "#0a5f5e" }}><ReceiptLongOutlinedIcon fontSize="small" /></Box><Box><Typography className="order-detail-value" fontWeight={800}>{item.productName}</Typography><Typography className="order-detail-mono" variant="caption" color="text.secondary">{item.productId}</Typography></Box></Stack>
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
              </Box>
            </Stack>
          ) : null}
        </DialogContent>
        <DialogActions sx={{ px: { xs: 2.5, md: 4 }, py: 2.25, borderTop: "1px solid", borderColor: "divider" }}>
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
          <Button variant="outlined" onClick={() => setSelectedOrder(null)}>Đóng</Button>
        </DialogActions>
      </Dialog>
    </MainLayout>
  );
}
