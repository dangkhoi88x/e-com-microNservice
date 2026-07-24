import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  LinearProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from "@mui/material";
import ArrowDownwardRoundedIcon from "@mui/icons-material/ArrowDownwardRounded";
import ArrowUpwardRoundedIcon from "@mui/icons-material/ArrowUpwardRounded";
import ChevronRightRoundedIcon from "@mui/icons-material/ChevronRightRounded";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import KeyboardArrowRightRoundedIcon from "@mui/icons-material/KeyboardArrowRightRounded";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import PaymentsOutlinedIcon from "@mui/icons-material/PaymentsOutlined";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import TimelineRoundedIcon from "@mui/icons-material/TimelineRounded";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { Badge } from "../components/ui/badge";
import { Card } from "../components/ui/card";
import { Button as ShadcnButton } from "../components/ui/button";
import { getCategories } from "../services/categoryService";
import { hasAnyRole } from "../services/authenticationService";
import { getMyNotifications } from "../services/notificationService";
import { getAllOrders, getMyOrders } from "../services/orderService";
import { getAllPayments, getMyPayments } from "../services/paymentService";
import { getProducts } from "../services/productService";
import { formatDateTime, formatRelativeTime } from "../utils/dateTimeUtils";

const orderStatuses = ["PENDING", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELLED"];

const statusTone = (status) => {
  switch (status) {
    case "COMPLETED":
      return { color: "#15803d", bg: "#dcfce7" };
    case "CONFIRMED":
      return { color: "#2563eb", bg: "#dbeafe" };
    case "SHIPPING":
      return { color: "#0f766e", bg: "#ccfbf1" };
    case "PENDING":
      return { color: "#b45309", bg: "#fef3c7" };
    default:
      return { color: "#64748b", bg: "#f1f5f9" };
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
    label: "Products in catalog",
    icon: <Inventory2OutlinedIcon />,
    iconColor: "#2563eb",
    iconBg: "#eaf1ff",
    delta: "+8.4%",
    note: "from last month",
    positive: true,
    path: "/products",
  },
  {
    key: "orders",
    label: "Total orders",
    icon: <ShoppingBagOutlinedIcon />,
    iconColor: "#b45309",
    iconBg: "#fff4df",
    delta: "+12.6%",
    note: "from last month",
    positive: true,
    path: "/orders",
  },
  {
    key: "payments",
    label: "Payment records",
    icon: <PaymentsOutlinedIcon />,
    iconColor: "#0f766e",
    iconBg: "#e1f8f1",
    delta: "+4.1%",
    note: "from last month",
    positive: true,
    path: "/payments",
  },
  {
    key: "notifications",
    label: "Unread notifications",
    icon: <NotificationsNoneOutlinedIcon />,
    iconColor: "#dc2626",
    iconBg: "#fff0f0",
    delta: "-2.8%",
    note: "from last month",
    positive: false,
    path: "/notifications",
  },
];

const templateCards = [
  {
    title: "Product catalog",
    description: "Manage products and inventory signals",
    path: "/products",
    className: "template-blue",
    icon: <Inventory2OutlinedIcon />,
    label: "Catalog",
  },
  {
    title: "Order operations",
    description: "Track status and customer purchases",
    path: "/orders",
    className: "template-green",
    icon: <ShoppingBagOutlinedIcon />,
    label: "Operations",
  },
  {
    title: "Payment review",
    description: "Review pending and completed payments",
    path: "/payments",
    className: "template-orange",
    icon: <PaymentsOutlinedIcon />,
    label: "Finance",
  },
  {
    title: "Promotion engine",
    description: "Create vouchers and manage campaign availability",
    path: "/promotions",
    className: "template-blue",
    icon: <LocalOfferOutlinedIcon />,
    label: "Promotions",
  },
];

function StatusPill({ status }) {
  const tone = statusTone(status);
  return (
    <Badge
      variant="outline"
      className="status-shadcn-badge"
      style={{ backgroundColor: tone.bg, color: tone.color, borderColor: tone.bg }}
    >
      {status || "UNKNOWN"}
    </Badge>
  );
}

function MetricCard({ metric, value, onClick }) {
  return (
    <Card className="dashboard-card metric-card" onClick={onClick}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
        <Box className="metric-icon" sx={{ color: metric.iconColor, bgcolor: metric.iconBg }}>
          {metric.icon}
        </Box>
        <IconButton size="small" aria-label={`Open ${metric.label}`}>
          <ChevronRightRoundedIcon fontSize="small" />
        </IconButton>
      </Stack>
      <Typography className="metric-label">{metric.label}</Typography>
      <Typography className="metric-value">{value}</Typography>
      <Stack direction="row" spacing={0.65} alignItems="center" className={metric.positive ? "delta positive" : "delta negative"}>
        {metric.positive ? <ArrowUpwardRoundedIcon /> : <ArrowDownwardRoundedIcon />}
        <span>{metric.delta}</span>
        <Typography component="span" className="delta-note">{metric.note}</Typography>
      </Stack>
    </Card>
  );
}

function TemplateCard({ template, onClick }) {
  return (
    <Card className="dashboard-card template-card" onClick={onClick}>
      <Box className={`template-visual ${template.className}`}>
        <Chip label="Active" size="small" className="template-status" />
        <Box className="template-stack template-stack-back" />
        <Box className="template-stack template-stack-front">
          <Box className="template-stack-line" />
          {template.icon}
        </Box>
      </Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1.5} sx={{ mt: 1.65 }}>
        <Box>
          <Typography className="template-title">{template.title}</Typography>
          <Typography className="template-description">{template.description}</Typography>
        </Box>
        <ChevronRightRoundedIcon className="template-arrow" />
      </Stack>
      <Stack direction="row" spacing={0.65} sx={{ mt: 1.5 }}>
        <Box className="platform-badge">P</Box>
        <Box className="platform-badge">O</Box>
        <Box className="platform-badge">A</Box>
        <Typography className="platform-caption">Connected services</Typography>
      </Stack>
    </Card>
  );
}

export default function Dashboard() {
  const navigate = useNavigate();
  const isAdmin = hasAnyRole("ROLE_ADMIN", "ADMIN");
  const [metrics, setMetrics] = useState({ products: 0, categories: 0, orders: 0, payments: 0, notifications: 0 });
  const [orders, setOrders] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [lastUpdated, setLastUpdated] = useState(new Date());

  const orderStatusCounts = useMemo(() => orderStatuses.reduce((result, status) => {
    result[status] = orders.filter((order) => order.status === status).length;
    return result;
  }, {}), [orders]);
  const loadDashboard = async () => {
    setLoading(true);
    setErrorMessage("");
    const orderRequest = isAdmin ? getAllOrders : getMyOrders;
    const paymentRequest = isAdmin ? getAllPayments : getMyPayments;
    const [productResult, categoryResult, orderResult, paymentResult, notificationResult] = await Promise.allSettled([
      getProducts({ page: 1, size: 1 }),
      getCategories(),
      orderRequest({ page: 1, size: 100 }),
      paymentRequest({ page: 1, size: 1 }),
      getMyNotifications(),
    ]);

    const productPage = productResult.status === "fulfilled" ? productResult.value : { totalElements: 0 };
    const categories = categoryResult.status === "fulfilled" ? categoryResult.value : [];
    const orderPage = orderResult.status === "fulfilled" ? orderResult.value : { content: [], totalElements: 0 };
    const paymentPage = paymentResult.status === "fulfilled" ? paymentResult.value : { totalElements: 0 };
    const notificationItems = notificationResult.status === "fulfilled" ? notificationResult.value : [];

    setMetrics({
      products: productPage.totalElements || 0,
      categories: categories.length || 0,
      orders: orderPage.totalElements || 0,
      payments: paymentPage.totalElements || 0,
      notifications: notificationItems.filter((item) => !item.read && !item.isRead).length || notificationItems.length || 0,
    });
    setOrders(orderPage.content || []);
    setNotifications(notificationItems || []);
    if ([productResult, categoryResult, orderResult].some((result) => result.status === "rejected")) {
      setErrorMessage("Some service data is unavailable. Showing the latest available dashboard data.");
    }
    setLastUpdated(new Date());
    setLoading(false);
  };

  useEffect(() => {
    loadDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const latestOrders = orders.slice(0, 6);
  const latestNotifications = notifications.slice(0, 4);
  const metricValues = { ...metrics, notifications: metrics.notifications || notifications.length };
  const completedRate = orders.length ? Math.round((orderStatusCounts.COMPLETED / orders.length) * 100) : 0;

  return (
    <MainLayout>
      <Box className="dashboard-page">
        <Stack className="dashboard-intro" direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ xs: "stretch", md: "flex-end" }} spacing={2}>
          <Box>
            <Typography className="eyebrow">Store operations / Overview</Typography>
            <Typography component="h1" className="dashboard-title">Welcome back, Admin.</Typography>
            <Typography className="dashboard-subtitle">A clear view of products, orders and payments across your e-commerce services.</Typography>
          </Box>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25} className="intro-actions">
            <ShadcnButton variant="outline" className="date-button"><TimelineRoundedIcon fontSize="small" />{lastUpdated.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })}</ShadcnButton>
          </Stack>
        </Stack>

        {!isAdmin && <Alert severity="info" className="dashboard-alert">You are viewing personal order and payment data. Login as admin to see the whole store.</Alert>}
        {errorMessage && <Alert severity="warning" className="dashboard-alert">{errorMessage}</Alert>}

        {loading ? (
          <Paper className="dashboard-card dashboard-loading">
            <CircularProgress size={28} />
            <Typography>Loading store overview...</Typography>
          </Paper>
        ) : (
          <>
            <Box className="metrics-grid">
              {metricCards.map((metric) => <MetricCard key={metric.key} metric={metric} value={metricValues[metric.key]} onClick={() => navigate(metric.path)} />)}
            </Box>

            <Box className="dashboard-section-heading">
              <Box>
                <Typography component="h2" className="section-title">Recent orders</Typography>
                <Typography className="section-subtitle">Order activity from the order-service</Typography>
              </Box>
              <Button className="text-action" endIcon={<KeyboardArrowRightRoundedIcon />} onClick={() => navigate("/orders")}>View all</Button>
            </Box>

            <Paper className="dashboard-card orders-card">
              <TableContainer>
                <Table className="orders-table" sx={{ minWidth: 760 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>ORDER</TableCell>
                      <TableCell>ORDER OWNER</TableCell>
                      <TableCell>STATUS</TableCell>
                      <TableCell align="right">TOTAL</TableCell>
                      <TableCell>CREATED</TableCell>
                      <TableCell align="right">ACTION</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {latestOrders.map((order, index) => (
                      <TableRow key={order.id || index} hover>
                        <TableCell>
                          <Stack direction="row" spacing={1.25} alignItems="center">
                            <Avatar className={`order-avatar avatar-${index % 4}`}><ShoppingBagOutlinedIcon fontSize="small" /></Avatar>
                            <Box><Typography className="table-primary">#{order.id?.slice(0, 8) || "pending"}</Typography><Typography className="table-secondary">{order.items?.length || 0} items</Typography></Box>
                          </Stack>
                        </TableCell>
                        <TableCell><Typography className="table-secondary owner-cell">{isAdmin ? order.userId?.slice(0, 14) || "Customer" : "My account"}</Typography></TableCell>
                        <TableCell><StatusPill status={order.status} /></TableCell>
                        <TableCell align="right"><Typography className="table-primary">{formatPrice(order.totalAmount)}</Typography></TableCell>
                        <TableCell><Typography className="table-secondary">{formatDateTime(order.createdAt)}</Typography></TableCell>
                        <TableCell align="right"><Button className="row-action" onClick={() => navigate("/orders")}>View</Button></TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              {latestOrders.length === 0 && <Box className="empty-state"><ShoppingBagOutlinedIcon /><Typography>No orders yet</Typography><Typography className="table-secondary">Create an order to see it here.</Typography></Box>}
              <Box className="table-pattern" />
            </Paper>

            <Box className="lower-grid">
              <Paper className="dashboard-card status-card">
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Box><Typography component="h2" className="section-title">Order health</Typography><Typography className="section-subtitle">Completion rate across loaded orders</Typography></Box>
                  <Box className="health-score">{completedRate}%</Box>
                </Stack>
                <LinearProgress variant="determinate" value={completedRate} className="health-progress" />
                <Stack spacing={1.25} sx={{ mt: 2.4 }}>
                  {orderStatuses.slice(0, 4).map((status) => {
                    const count = orderStatusCounts[status] || 0;
                    return <Stack key={status} direction="row" justifyContent="space-between" alignItems="center"><Stack direction="row" spacing={1} alignItems="center"><Box className={`status-dot status-${status.toLowerCase()}`} /><Typography className="table-secondary">{status}</Typography></Stack><Typography className="table-primary">{count}</Typography></Stack>;
                  })}
                </Stack>
              </Paper>

              <Paper className="dashboard-card notification-card">
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Box><Typography component="h2" className="section-title">Latest notifications</Typography><Typography className="section-subtitle">Recent updates from notification-service</Typography></Box>
                  <IconButton size="small" onClick={() => navigate("/notifications")} aria-label="Open notifications"><ChevronRightRoundedIcon /></IconButton>
                </Stack>
                <Stack spacing={1.2} sx={{ mt: 1.7 }}>
                  {latestNotifications.length === 0 ? <Typography className="table-secondary">No notifications yet.</Typography> : latestNotifications.map((notification, index) => <Stack direction="row" spacing={1.2} alignItems="center" key={notification.id || index}><Avatar className={`notification-avatar notification-${index % 3}`}><NotificationsNoneOutlinedIcon fontSize="small" /></Avatar><Box sx={{ minWidth: 0, flex: 1 }}><Typography className="table-primary notification-title">{notification.title || notification.type || "Store update"}</Typography><Typography className="table-secondary notification-message">{notification.message || "New account or order activity"}</Typography></Box><Typography className="time-label">{formatRelativeTime(notification.createdAt)}</Typography></Stack>)}
                </Stack>
              </Paper>
            </Box>

            <Box className="dashboard-section-heading templates-heading">
              <Box><Typography component="h2" className="section-title">Service shortcuts</Typography><Typography className="section-subtitle">Jump into the services powering your store</Typography></Box>
              <Tooltip title="Refresh dashboard"><IconButton onClick={loadDashboard} aria-label="Refresh dashboard"><RefreshRoundedIcon /></IconButton></Tooltip>
            </Box>
            <Box className="templates-grid">
              {templateCards.map((template) => <TemplateCard key={template.title} template={template} onClick={() => navigate(template.path)} />)}
            </Box>

            <Paper className="dashboard-card footer-summary">
              <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", sm: "center" }} spacing={2}><Stack direction="row" spacing={1.2} alignItems="center"><Box className="summary-icon"><StorefrontOutlinedIcon /></Box><Box><Typography className="table-primary">{metrics.categories} active categories</Typography><Typography className="table-secondary">Product service is connected and ready for catalog work.</Typography></Box></Stack><Button className="text-action" endIcon={<KeyboardArrowRightRoundedIcon />} onClick={() => navigate("/categories")}>Manage categories</Button></Stack>
            </Paper>
          </>
        )}
      </Box>
    </MainLayout>
  );
}
