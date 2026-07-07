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
import AdminPanelSettingsOutlinedIcon from "@mui/icons-material/AdminPanelSettingsOutlined";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { useEffect, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import { hasAnyRole } from "../services/authenticationService";
import {
  cancelPayment,
  getAllPayments,
  getMyPayments,
  markPaymentFailed,
  markPaymentSuccess,
} from "../services/paymentService";
import { formatDateTime } from "../utils/dateTimeUtils";

const paymentStatuses = ["ALL", "PENDING", "SUCCESS", "FAILED", "CANCELLED"];

const statusColor = (status) => {
  switch (status) {
    case "PENDING":
      return "warning";
    case "SUCCESS":
      return "success";
    case "FAILED":
      return "error";
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

export default function Payments() {
  const isAdmin = hasAnyRole("ROLE_ADMIN", "ADMIN");
  const [payments, setPayments] = useState([]);
  const [viewMode, setViewMode] = useState("mine");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [pageInfo, setPageInfo] = useState({
    currentPage: 1,
    pageSize: 10,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [selectedPayment, setSelectedPayment] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const visiblePayments =
    statusFilter === "ALL"
      ? payments
      : payments.filter((payment) => payment.status === statusFilter);

  const loadPayments = async (page = pageInfo.currentPage, mode = viewMode) => {
    setLoading(true);
    setErrorMessage("");

    try {
      const request = mode === "all" && isAdmin ? getAllPayments : getMyPayments;
      const data = await request({
        page,
        size: pageInfo.pageSize,
      });

      setPayments(data.content || []);
      setPageInfo({
        currentPage: data.currentPage || page,
        pageSize: data.pageSize || pageInfo.pageSize,
        totalPages: data.totalPages || 0,
        totalElements: data.totalElements || 0,
      });
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not load payments.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPayments(1, viewMode);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewMode]);

  const updatePaymentInList = (updatedPayment) => {
    setPayments((current) =>
      current.map((payment) =>
        payment.id === updatedPayment.id ? updatedPayment : payment,
      ),
    );

    if (selectedPayment?.id === updatedPayment.id) {
      setSelectedPayment(updatedPayment);
    }
  };

  const handleViewModeChange = () => {
    setViewMode((current) => (current === "mine" ? "all" : "mine"));
    setSelectedPayment(null);
    setStatusFilter("ALL");
  };

  const handleMarkSuccess = async (payment) => {
    if (!window.confirm(`Mark payment ${payment.id} as success?`)) return;

    setErrorMessage("");
    setSuccessMessage("");

    try {
      const updatedPayment = await markPaymentSuccess(payment.id);
      updatePaymentInList(updatedPayment);
      setSuccessMessage("Payment marked success.");
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not mark payment success.",
      );
    }
  };

  const handleMarkFailed = async (payment) => {
    if (!window.confirm(`Mark payment ${payment.id} as failed?`)) return;

    setErrorMessage("");
    setSuccessMessage("");

    try {
      const updatedPayment = await markPaymentFailed(payment.id);
      updatePaymentInList(updatedPayment);
      setSuccessMessage("Payment marked failed.");
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not mark payment failed.",
      );
    }
  };

  const handleCancelPayment = async (payment) => {
    if (!window.confirm(`Cancel payment ${payment.id}?`)) return;

    setErrorMessage("");
    setSuccessMessage("");

    try {
      const updatedPayment = await cancelPayment(payment.id);
      updatePaymentInList(updatedPayment);
      setSuccessMessage("Payment cancelled successfully.");
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not cancel payment.",
      );
    }
  };

  const renderStatus = (status) => (
    <Chip
      size="small"
      label={status || "UNKNOWN"}
      color={statusColor(status)}
      variant="outlined"
    />
  );

  const renderSimulationActions = (payment) => {
    if (payment.status !== "PENDING") return null;

    return (
      <>
        {isAdmin && (
          <>
            <Tooltip title="Simulate success">
              <IconButton color="success" onClick={() => handleMarkSuccess(payment)}>
                <CheckCircleOutlineOutlinedIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Simulate failed">
              <IconButton color="error" onClick={() => handleMarkFailed(payment)}>
                <ErrorOutlineOutlinedIcon />
              </IconButton>
            </Tooltip>
          </>
        )}
        <Tooltip title="Cancel payment">
          <IconButton color="default" onClick={() => handleCancelPayment(payment)}>
            <CancelOutlinedIcon />
          </IconButton>
        </Tooltip>
      </>
    );
  };

  return (
    <MainLayout>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        justifyContent="space-between"
        alignItems={{ xs: "stretch", sm: "center" }}
        spacing={2}
      >
        <Box>
          <Typography variant="h4" fontWeight={900}>
            Payments
          </Typography>
          <Typography color="text.secondary">
            Review payment records and simulate development payment outcomes.
          </Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          {isAdmin && (
            <Button
              variant={viewMode === "all" ? "contained" : "outlined"}
              startIcon={<AdminPanelSettingsOutlinedIcon />}
              onClick={handleViewModeChange}
            >
              {viewMode === "all" ? "My payments" : "All payments"}
            </Button>
          )}
          <Button
            variant="outlined"
            startIcon={<RefreshOutlinedIcon />}
            onClick={() => loadPayments(pageInfo.currentPage)}
          >
            Refresh
          </Button>
        </Stack>
      </Stack>

      <Paper
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
          {paymentStatuses.map((status) => (
            <Tab
              key={status}
              value={status}
              label={status === "ALL" ? "All" : status}
            />
          ))}
        </Tabs>

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
        ) : visiblePayments.length === 0 ? (
          <Box sx={{ p: 4 }}>
            <Typography fontWeight={800}>No payments found</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Change status filter or create a payment from an order.
            </Typography>
          </Box>
        ) : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Payment</TableCell>
                    <TableCell>Method</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Created At</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {visiblePayments.map((payment) => (
                    <TableRow key={payment.id} hover>
                      <TableCell>
                        <Typography fontWeight={800}>{payment.id}</Typography>
                        <Typography variant="body2" color="text.secondary">
                          Order {payment.orderId}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip size="small" label={payment.method || "-"} />
                      </TableCell>
                      <TableCell>{renderStatus(payment.status)}</TableCell>
                      <TableCell align="right">
                        {formatPrice(payment.amount)}
                      </TableCell>
                      <TableCell>{formatDateTime(payment.createdAt)}</TableCell>
                      <TableCell align="right">
                        <Tooltip title="View detail">
                          <IconButton onClick={() => setSelectedPayment(payment)}>
                            <VisibilityOutlinedIcon />
                          </IconButton>
                        </Tooltip>
                        {renderSimulationActions(payment)}
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
                {pageInfo.totalElements} payments
                {statusFilter !== "ALL" ? `, ${visiblePayments.length} shown` : ""}
              </Typography>
              <Pagination
                page={pageInfo.currentPage}
                count={Math.max(pageInfo.totalPages, 1)}
                color="primary"
                onChange={(_, page) => loadPayments(page)}
              />
            </Stack>
          </>
        )}
      </Paper>

      <Dialog
        open={Boolean(selectedPayment)}
        onClose={() => setSelectedPayment(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Payment detail</DialogTitle>
        <DialogContent dividers>
          {selectedPayment && (
            <Stack spacing={2.5}>
              <Stack
                direction={{ xs: "column", sm: "row" }}
                justifyContent="space-between"
                spacing={2}
              >
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Payment ID
                  </Typography>
                  <Typography fontWeight={900}>{selectedPayment.id}</Typography>
                </Box>
                <Box>{renderStatus(selectedPayment.status)}</Box>
              </Stack>

              <Stack direction={{ xs: "column", sm: "row" }} spacing={3}>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Amount
                  </Typography>
                  <Typography fontWeight={900}>
                    {formatPrice(selectedPayment.amount)}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Method
                  </Typography>
                  <Typography fontWeight={800}>
                    {selectedPayment.method || "-"}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Created At
                  </Typography>
                  <Typography fontWeight={800}>
                    {formatDateTime(selectedPayment.createdAt)}
                  </Typography>
                </Box>
              </Stack>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Order ID
                </Typography>
                <Typography fontWeight={800}>{selectedPayment.orderId}</Typography>
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Transaction Code
                </Typography>
                <Typography fontWeight={800}>
                  {selectedPayment.transactionCode || "-"}
                </Typography>
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Failure Reason
                </Typography>
                <Typography fontWeight={800}>
                  {selectedPayment.failureReason || "-"}
                </Typography>
              </Box>

              {viewMode === "all" && (
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    User ID
                  </Typography>
                  <Typography fontWeight={800}>{selectedPayment.userId}</Typography>
                </Box>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          {selectedPayment && renderSimulationActions(selectedPayment)}
          <Button onClick={() => setSelectedPayment(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </MainLayout>
  );
}
