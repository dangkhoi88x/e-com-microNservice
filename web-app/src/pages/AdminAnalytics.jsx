import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress, LinearProgress,
  MenuItem, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow,
  TextField, Typography,
} from "@mui/material";
import AttachMoneyOutlinedIcon from "@mui/icons-material/AttachMoneyOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import TrendingUpOutlinedIcon from "@mui/icons-material/TrendingUpOutlined";
import { useCallback, useEffect, useMemo, useState } from "react";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getAdminAnalytics } from "../services/orderService";
import "./AdminAnalytics.css";

const money = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
const dateInput = (date) => date.toISOString().slice(0, 10);
const statusLabels = { PENDING: "Chờ xử lý", PENDING_PAYMENT: "Chờ thanh toán", CONFIRMED: "Đã xác nhận", SHIPPING: "Đang giao", COMPLETED: "Hoàn tất", CANCELLED: "Đã huỷ", DELIVERY_FAILED: "Giao thất bại", RETURNING: "Hoàn hàng", RETURNED: "Đã hoàn" };
const statusColors = { PENDING: "#f59e0b", PENDING_PAYMENT: "#f97316", CONFIRMED: "#2563eb", SHIPPING: "#7c3aed", COMPLETED: "#0f8a76", CANCELLED: "#ef4444", DELIVERY_FAILED: "#dc2626", RETURNING: "#db2777", RETURNED: "#64748b" };

function dateRange(period) {
  const to = new Date();
  const from = new Date(to);
  from.setDate(from.getDate() - (period === "7" ? 6 : period === "90" ? 89 : 29));
  return { from: dateInput(from), to: dateInput(to) };
}

function RevenueChart({ points }) {
  const chart = useMemo(() => {
    const width = 760; const height = 235; const padding = { top: 22, right: 16, bottom: 30, left: 12 };
    const values = points.map((point) => Number(point.revenue || 0));
    const max = Math.max(...values, 1);
    const usableWidth = width - padding.left - padding.right;
    const usableHeight = height - padding.top - padding.bottom;
    const coordinates = values.map((value, index) => ({ x: padding.left + (values.length <= 1 ? usableWidth / 2 : (index * usableWidth) / (values.length - 1)), y: padding.top + usableHeight - (value / max) * usableHeight }));
    return { width, height, padding, max, coordinates, polyline: coordinates.map(({ x, y }) => `${x},${y}`).join(" ") };
  }, [points]);
  if (!points.length) return <Box className="analytics-empty-chart"><TrendingUpOutlinedIcon /><Typography>Chưa có đơn hoàn tất trong khoảng thời gian này.</Typography></Box>;
  return <Box className="revenue-chart"><Box className="chart-scale"><span>{money(chart.max)}</span><span>{money(chart.max / 2)}</span><span>0 đ</span></Box><svg viewBox={`0 0 ${chart.width} ${chart.height}`} role="img" aria-label="Biểu đồ doanh thu theo ngày" preserveAspectRatio="none"><defs><linearGradient id="revenue-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#12a594" stopOpacity=".24" /><stop offset="100%" stopColor="#12a594" stopOpacity="0" /></linearGradient></defs>{[0, .5, 1].map((line) => <line key={line} x1={chart.padding.left} x2={chart.width - chart.padding.right} y1={chart.padding.top + (chart.height - chart.padding.top - chart.padding.bottom) * line} y2={chart.padding.top + (chart.height - chart.padding.top - chart.padding.bottom) * line} className="chart-grid" />)}<polygon points={`${chart.padding.left},${chart.height - chart.padding.bottom} ${chart.polyline} ${chart.width - chart.padding.right},${chart.height - chart.padding.bottom}`} fill="url(#revenue-fill)" /><polyline points={chart.polyline} className="chart-line" />{chart.coordinates.map((point, index) => <circle key={points[index].date} cx={point.x} cy={point.y} r="4" className="chart-dot"><title>{`${points[index].date}: ${money(points[index].revenue)}`}</title></circle>)}</svg><Box className="chart-dates">{points.filter((_, index) => points.length <= 6 || index === 0 || index === points.length - 1 || index % Math.ceil(points.length / 5) === 0).map((point) => <span key={point.date}>{new Date(`${point.date}T00:00:00`).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" })}</span>)}</Box></Box>;
}

export default function AdminAnalytics() {
  const initial = dateRange("30");
  const [period, setPeriod] = useState("30");
  const [from, setFrom] = useState(initial.from);
  const [to, setTo] = useState(initial.to);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    if (!from || !to || from > to) { setError("Khoảng ngày không hợp lệ."); return; }
    setLoading(true);
    try {
      const toExclusive = new Date(`${to}T00:00:00`);
      toExclusive.setDate(toExclusive.getDate() + 1);
      setData(await getAdminAnalytics(new Date(`${from}T00:00:00`), toExclusive));
      setError("");
    } catch (requestError) { setError(requestError.response?.data?.message || "Không thể tải báo cáo từ order-service."); }
    finally { setLoading(false); }
  }, [from, to]);
  useEffect(() => { load(); }, [load]);
  const updatePeriod = (value) => { setPeriod(value); const range = dateRange(value); setFrom(range.from); setTo(range.to); };
  const statuses = Object.entries(data?.ordersByStatus || {}).sort(([, left], [, right]) => right - left);
  const highestStatus = Math.max(...statuses.map(([, count]) => count), 1);
  const cards = [
    { label: "Doanh thu", value: money(data?.revenue), caption: "Từ đơn hoàn tất", icon: <AttachMoneyOutlinedIcon /> },
    { label: "Tổng đơn hàng", value: data?.totalOrders || 0, caption: "Đơn được tạo trong kỳ", icon: <ShoppingBagOutlinedIcon /> },
    { label: "Đơn hoàn tất", value: data?.completedOrders || 0, caption: "Đã giao thành công", icon: <CheckCircleOutlineOutlinedIcon /> },
    { label: "Giá trị đơn TB", value: money(data?.averageOrderValue), caption: "Theo đơn hoàn tất", icon: <TrendingUpOutlinedIcon /> },
  ];

  return <MainLayout><Box className="admin-analytics-page"><PageHeader eyebrow="Workspace / Analytics" title="Phân tích bán hàng" description="Theo dõi doanh thu, đơn hàng và hiệu quả sản phẩm từ dữ liệu order-service." actions={<Stack direction={{ xs: "column", sm: "row" }} spacing={1}><TextField select size="small" value={period} onChange={(event) => updatePeriod(event.target.value)}><MenuItem value="7">7 ngày qua</MenuItem><MenuItem value="30">30 ngày qua</MenuItem><MenuItem value="90">90 ngày qua</MenuItem></TextField><Button variant="outlined" startIcon={<RefreshOutlinedIcon />} onClick={load}>Tải lại</Button></Stack>} />
    <Paper className="analytics-filter" elevation={0}><Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} alignItems={{ sm: "center" }}><Typography fontWeight={700}>Khoảng thời gian</Typography><TextField label="Từ ngày" type="date" size="small" value={from} onChange={(event) => { setPeriod(""); setFrom(event.target.value); }} slotProps={{ inputLabel: { shrink: true } }} /><TextField label="Đến ngày" type="date" size="small" value={to} onChange={(event) => { setPeriod(""); setTo(event.target.value); }} slotProps={{ inputLabel: { shrink: true } }} /><Typography className="analytics-filter-note">Doanh thu chỉ tính các đơn hoàn tất.</Typography></Stack></Paper>
    {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
    {loading ? <Paper className="analytics-loading"><CircularProgress size={30} /><Typography>Đang tổng hợp dữ liệu thực tế...</Typography></Paper> : <><Box className="analytics-kpi-grid">{cards.map((card) => <Card key={card.label} className="analytics-kpi" elevation={0}><CardContent><Box className="analytics-kpi-icon">{card.icon}</Box><Typography className="analytics-kpi-label">{card.label}</Typography><Typography className="analytics-kpi-value">{card.value}</Typography><Typography className="analytics-kpi-caption">{card.caption}</Typography></CardContent></Card>)}</Box>
      <Box className="analytics-main-grid"><Paper className="analytics-panel analytics-revenue-panel" elevation={0}><Stack direction="row" justifyContent="space-between" alignItems="flex-start"><Box><Typography className="analytics-panel-title">Doanh thu theo ngày</Typography><Typography className="analytics-panel-subtitle">Tổng tiền từ các đơn đã hoàn tất</Typography></Box><Chip label={`${data?.completedOrders || 0} đơn hoàn tất`} className="analytics-chip" /></Stack><RevenueChart points={data?.revenueByDate || []} /></Paper><Paper className="analytics-panel" elevation={0}><Typography className="analytics-panel-title">Trạng thái đơn hàng</Typography><Typography className="analytics-panel-subtitle">Phân bố đơn trong khoảng đã chọn</Typography><Stack spacing={1.65} sx={{ mt: 2.5 }}>{statuses.length ? statuses.map(([status, count]) => <Box key={status}><Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: .65 }}><Stack direction="row" spacing={.8} alignItems="center"><Box className="status-color-dot" sx={{ bgcolor: statusColors[status] || "#94a3b8" }} /><Typography className="analytics-status-label">{statusLabels[status] || status}</Typography></Stack><Typography fontWeight={800}>{count}</Typography></Stack><LinearProgress variant="determinate" value={(count / highestStatus) * 100} sx={{ height: 7, borderRadius: 9, bgcolor: "#edf2f4", "& .MuiLinearProgress-bar": { bgcolor: statusColors[status] || "#94a3b8", borderRadius: 9 } }} /></Box>) : <Typography color="text.secondary">Chưa có đơn hàng trong kỳ.</Typography>}</Stack></Paper></Box>
      <Paper className="analytics-panel analytics-products" elevation={0}><Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1}><Box><Typography className="analytics-panel-title">Top 5 sản phẩm bán chạy</Typography><Typography className="analytics-panel-subtitle">Xếp hạng theo doanh thu từ đơn hoàn tất</Typography></Box><Inventory2OutlinedIcon color="action" /></Stack><Table sx={{ mt: 1.5 }}><TableHead><TableRow><TableCell>Hạng</TableCell><TableCell>Sản phẩm</TableCell><TableCell align="right">Đã bán</TableCell><TableCell align="right">Doanh thu</TableCell></TableRow></TableHead><TableBody>{(data?.topProducts || []).map((product, index) => <TableRow key={product.productId}><TableCell><Box className="product-rank">{index + 1}</Box></TableCell><TableCell><Typography fontWeight={700}>{product.name}</Typography><Typography className="product-id">{product.productId}</Typography></TableCell><TableCell align="right">{product.quantitySold}</TableCell><TableCell align="right"><Typography fontWeight={800}>{money(product.revenue)}</Typography></TableCell></TableRow>)}{!(data?.topProducts || []).length && <TableRow><TableCell colSpan={4} align="center" sx={{ py: 4, color: "text.secondary" }}>Chưa có sản phẩm từ đơn hoàn tất.</TableCell></TableRow>}</TableBody></Table></Paper>
    </>}</Box></MainLayout>;
}
