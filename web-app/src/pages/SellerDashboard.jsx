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
  Tooltip,
  Typography,
} from "@mui/material";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import PaymentsOutlinedIcon from "@mui/icons-material/PaymentsOutlined";
import RateReviewOutlinedIcon from "@mui/icons-material/RateReviewOutlined";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card } from "../components/ui/card";
import MainLayout from "../layouts/MainLayout";
import { getMySellerProducts } from "../services/productService";

const statusColors = {
  ACTIVE: "success",
  PENDING_APPROVAL: "warning",
  DRAFT: "default",
  REJECTED: "error",
  INACTIVE: "default",
};

const operationCards = [
  { title: "Đơn hàng & doanh thu", description: "Cần API order theo shop để tính doanh thu chính xác.", icon: <ShoppingBagOutlinedIcon />, className: "template-green" },
  { title: "Tồn kho thực tế", description: "Inventory hiện chưa có màn hình lọc theo shop/variant.", icon: <Inventory2OutlinedIcon />, className: "template-blue" },
  { title: "Voucher của shop", description: "Cần mô hình campaign do seller sở hữu trước khi tạo voucher.", icon: <LocalOfferOutlinedIcon />, className: "template-orange" },
  { title: "Đánh giá khách hàng", description: "Cần API review theo shop và luồng trả lời đánh giá.", icon: <RateReviewOutlinedIcon />, className: "template-blue" },
  { title: "Đối soát thanh toán", description: "Cần settlement theo đơn hoàn thành, không dùng số liệu payment thô.", icon: <PaymentsOutlinedIcon />, className: "template-green" },
];

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
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [lastUpdated, setLastUpdated] = useState(new Date());

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const page = await getMySellerProducts({ page: 1, size: 100 });
      setProducts(page.content || []);
      setError("");
      setLastUpdated(new Date());
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể tải dữ liệu vận hành của shop.");
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
      drafts: count("DRAFT") + count("REJECTED"),
      lowStock: activeProducts.filter((product) => Number(product.quantity || 0) <= 5).length,
      inventoryUnits: activeProducts.reduce((total, product) => total + Number(product.quantity || 0), 0),
    };
  }, [products]);

  return (
    <MainLayout>
      <Box className="dashboard-page">
        <Stack className="dashboard-intro" direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ xs: "stretch", md: "flex-end" }} spacing={2}>
          <Box>
            <Typography className="eyebrow">Seller center / Overview</Typography>
            <Typography component="h1" className="dashboard-title">Vận hành shop</Typography>
            <Typography className="dashboard-subtitle">Theo dõi catalog của shop trước; các nghiệp vụ đơn hàng, doanh thu và đối soát sẽ được nối khi có API seller riêng.</Typography>
          </Box>
          <Tooltip title="Tải lại dữ liệu"><Button variant="outlined" startIcon={<RefreshRoundedIcon />} onClick={load}>{lastUpdated.toLocaleDateString("vi-VN")}</Button></Tooltip>
        </Stack>

        <Alert severity="info" className="dashboard-alert">Số liệu tồn kho bên dưới là tín hiệu từ Product-service. Inventory-service vẫn là nguồn tồn kho chính thức.</Alert>
        {error && <Alert severity="error" className="dashboard-alert">{error}</Alert>}

        {loading ? <Paper className="dashboard-card dashboard-loading"><CircularProgress size={28} /><Typography>Đang tải seller center...</Typography></Paper> : <>
          <Box className="metrics-grid">
            <MetricCard icon={<StorefrontOutlinedIcon />} color="#15803d" background="#dcfce7" label="Sản phẩm đang bán" value={summary.active} note="Đã được duyệt và công khai" onClick={() => navigate("/seller/products")} />
            <MetricCard icon={<ShoppingBagOutlinedIcon />} color="#b45309" background="#fff4df" label="Đang chờ duyệt" value={summary.pending} note="Sản phẩm đã gửi kiểm duyệt" onClick={() => navigate("/seller/products")} />
            <MetricCard icon={<Inventory2OutlinedIcon />} color="#dc2626" background="#fff0f0" label="Sắp hết hàng" value={summary.lowStock} note="Sản phẩm active còn tối đa 5 đơn vị" onClick={() => navigate("/seller/products")} />
            <MetricCard icon={<Inventory2OutlinedIcon />} color="#2563eb" background="#eaf1ff" label="Đơn vị hàng khả dụng" value={summary.inventoryUnits} note="Tổng quantity của catalog active" onClick={() => navigate("/seller/products")} />
          </Box>

          <Box className="dashboard-section-heading">
            <Box><Typography component="h2" className="section-title">Sản phẩm gần đây</Typography><Typography className="section-subtitle">Dữ liệu thật từ Product-service của shop này</Typography></Box>
            <Button className="text-action" onClick={() => navigate("/seller/products")}>Quản lý sản phẩm</Button>
          </Box>
          <Paper className="dashboard-card orders-card">
            <TableContainer><Table className="orders-table" sx={{ minWidth: 700 }}><TableHead><TableRow><TableCell>SẢN PHẨM</TableCell><TableCell>TRẠNG THÁI</TableCell><TableCell align="right">GIÁ</TableCell><TableCell align="right">KHO</TableCell></TableRow></TableHead>
              <TableBody>{products.slice(0, 6).map((product) => <TableRow key={product.id} hover><TableCell><Typography className="table-primary">{product.name}</Typography><Typography className="table-secondary">{product.slug}</Typography></TableCell><TableCell><Chip label={product.status} size="small" color={statusColors[product.status] || "default"} /></TableCell><TableCell align="right"><Typography className="table-primary">{Number(product.price || 0).toLocaleString("vi-VN")} đ</Typography></TableCell><TableCell align="right"><Typography className="table-primary">{product.quantity || 0}</Typography></TableCell></TableRow>)}</TableBody></Table></TableContainer>
            {products.length === 0 && <Box className="empty-state"><Inventory2OutlinedIcon /><Typography>Shop chưa có sản phẩm</Typography><Button variant="contained" onClick={() => navigate("/seller/products/new")}>Tạo sản phẩm đầu tiên</Button></Box>}
          </Paper>

          <Box className="dashboard-section-heading templates-heading"><Box><Typography component="h2" className="section-title">Nghiệp vụ seller sắp kết nối</Typography><Typography className="section-subtitle">Không hiển thị số liệu giả trước khi backend bảo đảm dữ liệu theo shop.</Typography></Box></Box>
          <Box className="templates-grid">{operationCards.map((operation) => <Card key={operation.title} className="dashboard-card template-card"><Box className={`template-visual ${operation.className}`}><Chip label="Cần API seller" size="small" className="template-status" /><Box className="template-stack template-stack-back" /><Box className="template-stack template-stack-front"><Box className="template-stack-line" />{operation.icon}</Box></Box><Typography className="template-title" sx={{ mt: 1.65 }}>{operation.title}</Typography><Typography className="template-description">{operation.description}</Typography></Card>)}</Box>

          <Paper className="dashboard-card footer-summary"><Stack direction="row" spacing={1.2} alignItems="center"><Box className="summary-icon"><StorefrontOutlinedIcon /></Box><Box><Typography className="table-primary">{summary.drafts} sản phẩm cần hoàn thiện hoặc xử lý phản hồi</Typography><Typography className="table-secondary">Bản nháp và sản phẩm bị từ chối chỉ được seller sửa rồi gửi duyệt lại.</Typography></Box></Stack></Paper>
        </>}
      </Box>
    </MainLayout>
  );
}
