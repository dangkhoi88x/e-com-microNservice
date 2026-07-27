import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, Box, Button, Chip, CircularProgress, FormControl, InputLabel, MenuItem, Paper, Select, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from "@mui/material";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getSellerOrders, updateSellerOrderStatus } from "../services/orderService";

const statusColor = { PENDING: "warning", PENDING_PAYMENT: "warning", CONFIRMED: "info", SHIPPING: "primary", COMPLETED: "success", CANCELLED: "error", RETURNED: "error", RETURNING: "warning", DELIVERY_FAILED: "error" };
const money = (value) => `${Number(value || 0).toLocaleString("vi-VN")} đ`;

export default function SellerOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [updatingId, setUpdatingId] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    try { const page = await getSellerOrders({ page: 1, size: 100 }); setOrders(page.content || []); setError(""); }
    catch (requestError) { setError(requestError.response?.data?.message || "Không thể tải đơn hàng của shop."); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);
  const visible = useMemo(() => orders.filter((order) => `${order.orderCode || ""} ${order.shippingAddress || ""}`.toLowerCase().includes(query.toLowerCase()) && (!status || order.status === status)), [orders, query, status]);
  const ship = async (order) => {
    if (!window.confirm(`Bàn giao đơn ${order.orderCode || order.id} cho đơn vị vận chuyển?`)) return;
    setUpdatingId(order.id);
    try { await updateSellerOrderStatus(order.id, "SHIPPING"); await load(); }
    catch (requestError) { setError(requestError.response?.data?.message || "Không thể cập nhật trạng thái đơn hàng."); }
    finally { setUpdatingId(""); }
  };

  return <MainLayout>
    <PageHeader eyebrow="Seller center" title="Quản lý đơn hàng" description="Theo dõi các đơn thuộc shop, trạng thái thanh toán và tiến độ giao hàng." actions={<Button variant="outlined" startIcon={<RefreshOutlinedIcon />} onClick={load}>Tải lại</Button>} />
    {error && <Alert severity="error" sx={{ mt: 3 }} onClose={() => setError("")}>{error}</Alert>}
    <Paper className="admin-filter-panel" elevation={0} sx={{ mt: 3, p: 2 }}><Stack direction={{ xs: "column", md: "row" }} spacing={2}><TextField label="Tìm mã đơn hoặc địa chỉ" value={query} onChange={(event) => setQuery(event.target.value)} sx={{ flex: 1 }} /><FormControl sx={{ minWidth: 210 }}><InputLabel>Trạng thái</InputLabel><Select label="Trạng thái" value={status} onChange={(event) => setStatus(event.target.value)}><MenuItem value="">Tất cả</MenuItem>{["PENDING_PAYMENT", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELLED", "RETURNED"].map((value) => <MenuItem key={value} value={value}>{value}</MenuItem>)}</Select></FormControl></Stack></Paper>
    <Paper className="admin-data-panel" elevation={0} sx={{ mt: 3, overflow: "hidden" }}>{loading ? <Box sx={{ minHeight: 260, display: "grid", placeItems: "center" }}><CircularProgress /></Box> : <TableContainer><Table sx={{ minWidth: 940 }}><TableHead><TableRow><TableCell>Mã đơn</TableCell><TableCell>Sản phẩm</TableCell><TableCell>Thanh toán</TableCell><TableCell>Vận chuyển</TableCell><TableCell>Tổng tiền</TableCell><TableCell align="right">Thao tác</TableCell></TableRow></TableHead><TableBody>{visible.map((order) => <TableRow key={order.id} hover><TableCell><Typography fontWeight={800}>{order.orderCode || order.id.slice(0, 8)}</Typography><Typography variant="caption" color="text.secondary">{new Date(order.createdAt).toLocaleString("vi-VN")}</Typography></TableCell><TableCell><Typography fontWeight={700}>{order.items?.map((item) => `${item.productName} ×${item.quantity}`).join(", ") || "—"}</Typography><Typography variant="caption" color="text.secondary">{order.shippingAddress}</Typography></TableCell><TableCell><Chip size="small" label={order.status === "PENDING_PAYMENT" ? "Chờ thanh toán" : ["CONFIRMED", "SHIPPING", "COMPLETED"].includes(order.status) ? "Đã thanh toán" : order.status} color={statusColor[order.status] || "default"} /></TableCell><TableCell><Chip size="small" label={order.status} color={statusColor[order.status] || "default"} /></TableCell><TableCell><Typography fontWeight={800}>{money(order.totalAmount)}</Typography></TableCell><TableCell align="right">{order.status === "CONFIRMED" ? <Button size="small" variant="contained" startIcon={<LocalShippingOutlinedIcon />} disabled={updatingId === order.id} onClick={() => ship(order)}>{updatingId === order.id ? "Đang cập nhật" : "Bàn giao shipper"}</Button> : <Typography variant="caption" color="text.secondary">{order.status === "SHIPPING" ? "Đang giao" : "—"}</Typography>}</TableCell></TableRow>)}{!visible.length && <TableRow><TableCell colSpan={6} align="center" sx={{ py: 7 }}><Typography fontWeight={700}>Chưa có đơn hàng phù hợp.</Typography></TableCell></TableRow>}</TableBody></Table></TableContainer>}</Paper>
  </MainLayout>;
}
