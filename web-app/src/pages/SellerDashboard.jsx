import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
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
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import LocalFireDepartmentOutlinedIcon from "@mui/icons-material/LocalFireDepartmentOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import TaskAltOutlinedIcon from "@mui/icons-material/TaskAltOutlined";
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined";
import { useNavigate } from "react-router-dom";
import { Card } from "../components/ui/card";
import MainLayout from "../layouts/MainLayout";
import { getMySellerProducts } from "../services/productService";
import { getMySellerShop } from "../services/sellerService";

const productStatusColor = {
  ACTIVE: "success",
  PENDING_APPROVAL: "warning",
  DRAFT: "default",
  REJECTED: "error",
  INACTIVE: "default",
};

function MetricCard({ icon, color, background, label, value, note, onClick }) {
  return (
    <Card className="dashboard-card metric-card" onClick={onClick}>
      <Box className="metric-icon" sx={{ color, bgcolor: background }}>{icon}</Box>
      <Typography className="metric-label">{label}</Typography>
      <Typography className="metric-value">{value}</Typography>
      <Typography className="delta-note" sx={{ mt: 0.8 }}>{note}</Typography>
    </Card>
  );
}

export default function SellerDashboard() {
  const navigate = useNavigate();
  const [shop, setShop] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [lastUpdated, setLastUpdated] = useState(new Date());

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [shopData, productPage] = await Promise.all([
        getMySellerShop(),
        getMySellerProducts({ page: 1, size: 100 }),
      ]);
      setShop(shopData);
      setProducts(productPage.content || []);
      setError("");
      setLastUpdated(new Date());
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể tải dữ liệu seller center.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const summary = useMemo(() => {
    const count = (status) => products.filter((product) => product.status === status).length;
    const activeProducts = products.filter((product) => product.status === "ACTIVE");
    return {
      active: activeProducts.length,
      pending: count("PENDING_APPROVAL"),
      drafts: count("DRAFT"),
      rejected: count("REJECTED"),
      lowStock: activeProducts.filter((product) => Number(product.quantity || 0) <= 5).length,
    };
  }, [products]);

  const attentionProducts = products.filter((product) => ["DRAFT", "REJECTED"].includes(product.status));
  const recentProducts = [...products]
    .sort((left, right) => new Date(right.updatedAt || right.createdAt || 0) - new Date(left.updatedAt || left.createdAt || 0))
    .slice(0, 6);

  return (
    <MainLayout>
      <Box className="dashboard-page">
        <Stack className="dashboard-intro" direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ xs: "stretch", md: "flex-end" }} spacing={2}>
          <Box>
            <Typography className="eyebrow">Seller center / Overview</Typography>
            <Typography component="h1" className="dashboard-title">{shop?.shopName || "Cửa hàng của tôi"}</Typography>
            <Typography className="dashboard-subtitle">Theo dõi trạng thái shop, catalog và những việc cần xử lý.</Typography>
          </Box>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
            <Button variant="outlined" startIcon={<RefreshRoundedIcon />} onClick={load}>Cập nhật {lastUpdated.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}</Button>
            <Button variant="outlined" startIcon={<LocalFireDepartmentOutlinedIcon />} onClick={() => navigate("/seller/promotions")}>Khuyến mãi</Button>
            <Button variant="outlined" startIcon={<ShoppingBagOutlinedIcon />} onClick={() => navigate("/seller/orders")}>Đơn hàng</Button>
            <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => navigate("/seller/products/new")}>Tạo sản phẩm</Button>
          </Stack>
        </Stack>

        {error && <Alert severity="error" className="dashboard-alert">{error}</Alert>}

        {loading ? (
          <Paper className="dashboard-card dashboard-loading"><CircularProgress size={28} /><Typography>Đang tải seller center...</Typography></Paper>
        ) : (
          <>
            <Paper className="dashboard-card" elevation={0} sx={{ p: 2.5, mb: 3 }}>
              <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ md: "center" }} spacing={2}>
                <Stack direction="row" spacing={2} alignItems="center">
                  <Box sx={{ width: 52, height: 52, borderRadius: 3, bgcolor: "#eaf1ff", color: "#2563eb", display: "grid", placeItems: "center" }}><StorefrontOutlinedIcon /></Box>
                  <Box>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography fontWeight={900} fontSize={18}>{shop?.shopName}</Typography>
                      <Chip size="small" label={shop?.status || "UNKNOWN"} color={shop?.status === "APPROVED" ? "success" : "warning"} />
                    </Stack>
                    <Typography variant="body2" color="text.secondary">{shop?.address}, {shop?.city} · {shop?.phone}</Typography>
                  </Box>
                </Stack>
                <Button startIcon={<EditOutlinedIcon />} onClick={() => navigate("/seller/shop")}>Chỉnh sửa shop</Button>
              </Stack>
            </Paper>

            <Box className="metrics-grid">
              <MetricCard icon={<TaskAltOutlinedIcon />} color="#15803d" background="#dcfce7" label="Đang bán" value={summary.active} note="Sản phẩm đã duyệt và công khai" onClick={() => navigate("/seller/products")} />
              <MetricCard icon={<RefreshRoundedIcon />} color="#b45309" background="#fff4df" label="Chờ duyệt" value={summary.pending} note="Đang được admin kiểm duyệt" onClick={() => navigate("/seller/products")} />
              <MetricCard icon={<WarningAmberOutlinedIcon />} color="#dc2626" background="#fff0f0" label="Cần xử lý" value={summary.drafts + summary.rejected} note={`${summary.rejected} bị từ chối · ${summary.drafts} bản nháp`} onClick={() => navigate("/seller/products")} />
              <MetricCard icon={<Inventory2OutlinedIcon />} color="#2563eb" background="#eaf1ff" label="Sắp hết hàng" value={summary.lowStock} note="Sản phẩm active còn tối đa 5 đơn vị" onClick={() => navigate("/seller/products")} />
            </Box>

            <Box className="dashboard-section-heading">
              <Box>
                <Typography component="h2" className="section-title">Việc cần xử lý</Typography>
                <Typography className="section-subtitle">Hoàn thiện bản nháp hoặc sửa sản phẩm theo phản hồi kiểm duyệt.</Typography>
              </Box>
              <Button className="text-action" onClick={() => navigate("/seller/products")}>Xem tất cả sản phẩm</Button>
            </Box>
            <Paper className="dashboard-card orders-card">
              {attentionProducts.length ? (
                <TableContainer>
                  <Table sx={{ minWidth: 680 }}>
                    <TableHead><TableRow><TableCell>SẢN PHẨM</TableCell><TableCell>TRẠNG THÁI</TableCell><TableCell>PHẢN HỒI</TableCell><TableCell align="right">THAO TÁC</TableCell></TableRow></TableHead>
                    <TableBody>
                      {attentionProducts.slice(0, 5).map((product) => (
                        <TableRow key={product.id} hover>
                          <TableCell><Typography fontWeight={800}>{product.name}</Typography></TableCell>
                          <TableCell><Chip label={product.status} size="small" color={productStatusColor[product.status] || "default"} /></TableCell>
                          <TableCell><Typography variant="body2" color={product.moderationNote ? "error" : "text.secondary"}>{product.moderationNote || "Chưa gửi kiểm duyệt"}</Typography></TableCell>
                          <TableCell align="right"><Button size="small" onClick={() => navigate("/seller/products")}>Xử lý</Button></TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Box className="empty-state"><TaskAltOutlinedIcon /><Typography>Không có sản phẩm cần xử lý.</Typography></Box>
              )}
            </Paper>

            <Box className="dashboard-section-heading">
              <Box>
                <Typography component="h2" className="section-title">Sản phẩm cập nhật gần đây</Typography>
                <Typography className="section-subtitle">Dữ liệu thật từ catalog của shop.</Typography>
              </Box>
            </Box>
            <Paper className="dashboard-card orders-card">
              {recentProducts.length ? (
                <TableContainer>
                  <Table sx={{ minWidth: 680 }}>
                    <TableHead><TableRow><TableCell>SẢN PHẨM</TableCell><TableCell>TRẠNG THÁI</TableCell><TableCell align="right">GIÁ</TableCell><TableCell align="right">KHO</TableCell></TableRow></TableHead>
                    <TableBody>
                      {recentProducts.map((product) => (
                        <TableRow key={product.id} hover>
                          <TableCell><Typography fontWeight={800}>{product.name}</Typography><Typography variant="caption" color="text.secondary">{product.slug}</Typography></TableCell>
                          <TableCell><Chip label={product.status} size="small" color={productStatusColor[product.status] || "default"} /></TableCell>
                          <TableCell align="right">{Number(product.price || 0).toLocaleString("vi-VN")} đ</TableCell>
                          <TableCell align="right">{product.quantity || 0}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Box className="empty-state"><Inventory2OutlinedIcon /><Typography>Shop chưa có sản phẩm.</Typography><Button variant="contained" onClick={() => navigate("/seller/products/new")}>Tạo sản phẩm đầu tiên</Button></Box>
              )}
            </Paper>

            <Alert severity="info" sx={{ mt: 3 }}>
              Đơn hàng, doanh thu, voucher và đánh giá sẽ được bổ sung khi các service hỗ trợ truy vấn theo shop. Dashboard hiện chỉ hiển thị dữ liệu seller đã được backend bảo đảm.
            </Alert>
          </>
        )}
      </Box>
    </MainLayout>
  );
}
