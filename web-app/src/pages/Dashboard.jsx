import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import ShoppingCartOutlinedIcon from "@mui/icons-material/ShoppingCartOutlined";
import { useEffect, useMemo, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import { getCategories } from "../services/categoryService";
import { hasAnyRole } from "../services/authenticationService";
import { getMyNotifications } from "../services/notificationService";
import { getAllOrders, getMyOrders } from "../services/orderService";
import { getProducts } from "../services/productService";
import { formatDateTime, formatRelativeTime } from "../utils/dateTimeUtils";

const orderStatuses = [
  "PENDING",
  "CONFIRMED",
  "SHIPPING",
  "COMPLETED",
  "CANCELLED",
];

const statusColor = (status) => {
  switch (status) {
    case "PENDING":
      return "warning";
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

const metricCards = [
  {
    key: "products",
    label: "Total products",
    icon: <Inventory2OutlinedIcon />,
    color: "primary.main",
  },
  {
    key: "categories",
    label: "Total categories",
    icon: <CategoryOutlinedIcon />,
    color: "success.main",
  },
  {
    key: "orders",
    label: "Total orders",
    icon: <ShoppingCartOutlinedIcon />,
    color: "warning.main",
  },
];

export default function Dashboard() {
  const isAdmin = hasAnyRole("ROLE_ADMIN", "ADMIN");
  const [metrics, setMetrics] = useState({
    products: 0,
    categories: 0,
    orders: 0,
  });
  const [orders, setOrders] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  const orderStatusCounts = useMemo(() => {
    return orderStatuses.reduce((result, status) => {
      result[status] = orders.filter((order) => order.status === status).length;
      return result;
    }, {});
  }, [orders]);

  const maxStatusCount = Math.max(...Object.values(orderStatusCounts), 1);

  const loadDashboard = async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const orderRequest = isAdmin ? getAllOrders : getMyOrders;
      const [productPage, categories, orderPage, notificationItems] =
        await Promise.all([
          getProducts({ page: 1, size: 1 }),
          getCategories(),
          orderRequest({ page: 1, size: 100 }),
          getMyNotifications(),
        ]);

      setMetrics({
        products: productPage.totalElements || 0,
        categories: categories.length || 0,
        orders: orderPage.totalElements || 0,
      });
      setOrders(orderPage.content || []);
      setNotifications(notificationItems || []);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not load dashboard data.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const latestOrders = orders.slice(0, 5);
  const latestNotifications = notifications.slice(0, 5);

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
            Dashboard
          </Typography>
          <Typography color="text.secondary">
            Overview of catalog, orders, stock movement, and notifications.
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshOutlinedIcon />}
          onClick={loadDashboard}
        >
          Refresh
        </Button>
      </Stack>

      {!isAdmin && (
        <Alert severity="info" sx={{ mt: 3 }}>
          You are viewing personal order data. Login as admin to see all orders.
        </Alert>
      )}

      {errorMessage && (
        <Alert severity="error" sx={{ mt: 3 }}>
          {errorMessage}
        </Alert>
      )}

      {loading ? (
        <Paper
          elevation={0}
          sx={{ mt: 3, p: 6, border: "1px solid", borderColor: "divider" }}
        >
          <Stack alignItems="center" spacing={2}>
            <CircularProgress />
            <Typography color="text.secondary">Loading dashboard...</Typography>
          </Stack>
        </Paper>
      ) : (
        <>
          <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
            {metricCards.map((metric) => (
              <Grid item xs={12} md={4} key={metric.key}>
                <Paper
                  elevation={0}
                  sx={{
                    p: 2.5,
                    border: "1px solid",
                    borderColor: "divider",
                    height: "100%",
                  }}
                >
                  <Stack direction="row" spacing={2} alignItems="center">
                    <Box
                      sx={{
                        width: 48,
                        height: 48,
                        borderRadius: 2,
                        display: "grid",
                        placeItems: "center",
                        color: metric.color,
                        bgcolor: "action.hover",
                      }}
                    >
                      {metric.icon}
                    </Box>
                    <Box>
                      <Typography color="text.secondary">
                        {metric.label}
                      </Typography>
                      <Typography variant="h4" fontWeight={900}>
                        {metrics[metric.key]}
                      </Typography>
                    </Box>
                  </Stack>
                </Paper>
              </Grid>
            ))}
          </Grid>

          <Grid container spacing={2.5} sx={{ mt: 0 }}>
            <Grid item xs={12} lg={4}>
              <Paper
                elevation={0}
                sx={{
                  p: 2.5,
                  border: "1px solid",
                  borderColor: "divider",
                  height: "100%",
                }}
              >
                <Typography fontWeight={900}>Orders by status</Typography>
                <Stack spacing={2} sx={{ mt: 2 }}>
                  {orderStatuses.map((status) => {
                    const count = orderStatusCounts[status] || 0;
                    const progress = (count / maxStatusCount) * 100;

                    return (
                      <Box key={status}>
                        <Stack
                          direction="row"
                          justifyContent="space-between"
                          alignItems="center"
                          sx={{ mb: 0.75 }}
                        >
                          <Chip
                            size="small"
                            label={status}
                            color={statusColor(status)}
                            variant="outlined"
                          />
                          <Typography fontWeight={800}>{count}</Typography>
                        </Stack>
                        <LinearProgress
                          variant="determinate"
                          value={progress}
                          color={statusColor(status)}
                          sx={{ height: 8, borderRadius: 999 }}
                        />
                      </Box>
                    );
                  })}
                </Stack>
              </Paper>
            </Grid>

            <Grid item xs={12} lg={8}>
              <Paper
                elevation={0}
                sx={{
                  border: "1px solid",
                  borderColor: "divider",
                  overflow: "hidden",
                  height: "100%",
                }}
              >
                <Box sx={{ p: 2.5 }}>
                  <Typography fontWeight={900}>Latest orders</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Newest orders sorted by created time.
                  </Typography>
                </Box>
                <Divider />
                {latestOrders.length === 0 ? (
                  <Box sx={{ p: 3 }}>
                    <Typography fontWeight={800}>No orders yet</Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                      Create an order to see it here.
                    </Typography>
                  </Box>
                ) : (
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell>Order</TableCell>
                          <TableCell>Status</TableCell>
                          <TableCell align="right">Total</TableCell>
                          <TableCell>Created At</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {latestOrders.map((order) => (
                          <TableRow key={order.id} hover>
                            <TableCell>
                              <Typography fontWeight={800}>
                                {order.id?.slice(0, 8) || "-"}
                              </Typography>
                              <Typography variant="body2" color="text.secondary">
                                {order.items?.length || 0} items
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Chip
                                size="small"
                                label={order.status}
                                color={statusColor(order.status)}
                                variant="outlined"
                              />
                            </TableCell>
                            <TableCell align="right">
                              {formatPrice(order.totalAmount)}
                            </TableCell>
                            <TableCell>{formatDateTime(order.createdAt)}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </Paper>
            </Grid>
          </Grid>

          <Paper
            elevation={0}
            sx={{
              mt: 2.5,
              border: "1px solid",
              borderColor: "divider",
              overflow: "hidden",
            }}
          >
            <Box sx={{ p: 2.5 }}>
              <Stack direction="row" spacing={1.25} alignItems="center">
                <NotificationsOutlinedIcon color="primary" />
                <Box>
                  <Typography fontWeight={900}>Latest notifications</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Recent account and order notifications.
                  </Typography>
                </Box>
              </Stack>
            </Box>
            <Divider />
            {latestNotifications.length === 0 ? (
              <Box sx={{ p: 3 }}>
                <Typography fontWeight={800}>No notifications yet</Typography>
                <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                  Create, cancel, or update an order to generate notifications.
                </Typography>
              </Box>
            ) : (
              <List disablePadding>
                {latestNotifications.map((notification, index) => (
                  <Box key={notification.id || `${notification.type}-${index}`}>
                    <ListItem sx={{ px: 2.5, py: 1.75 }}>
                      <ListItemText
                        primary={
                          <Stack
                            direction={{ xs: "column", sm: "row" }}
                            justifyContent="space-between"
                            spacing={1}
                          >
                            <Typography fontWeight={900}>
                              {notification.title || notification.type}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              {formatRelativeTime(notification.createdAt)}
                            </Typography>
                          </Stack>
                        }
                        secondary={
                          <Typography color="text.secondary">
                            {notification.message || "-"}
                          </Typography>
                        }
                      />
                    </ListItem>
                    {index < latestNotifications.length - 1 && <Divider />}
                  </Box>
                ))}
              </List>
            )}
          </Paper>
        </>
      )}
    </MainLayout>
  );
}
