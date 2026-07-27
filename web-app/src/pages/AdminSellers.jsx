import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from "@mui/material";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import DoNotDisturbOnOutlinedIcon from "@mui/icons-material/DoNotDisturbOnOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getSellerShopsForAdmin, reviewSellerShop } from "../services/sellerService";

const statusTone = { PENDING: "warning", APPROVED: "success", REJECTED: "error", SUSPENDED: "default" };

export default function AdminSellers() {
  const [shops, setShops] = useState([]);
  const [status, setStatus] = useState("PENDING");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [review, setReview] = useState(null);
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const page = await getSellerShopsForAdmin({ status: status || undefined, page: 0, size: 50 });
      setShops(page.content || []);
      setError("");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể tải danh sách shop.");
    } finally { setLoading(false); }
  }, [status]);

  useEffect(() => { load(); }, [load]);
  const openReview = (shop, action) => { setReview({ shop, action }); setNote(""); };
  const closeReview = () => { if (!submitting) setReview(null); };
  const submitReview = async () => {
    if (!review) return;
    if (review.action === "REJECT" && !note.trim()) { setError("Cần nhập lý do từ chối."); return; }
    setSubmitting(true);
    try {
      await reviewSellerShop(review.shop.id, { action: review.action, note: note.trim() || null });
      setReview(null);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể cập nhật trạng thái shop.");
    } finally { setSubmitting(false); }
  };

  const reviewingReject = review?.action === "REJECT";
  return <MainLayout>
    <PageHeader eyebrow="Seller management" title="Duyệt đăng ký seller" description="Xem xét hồ sơ shop trước khi cấp quyền Seller cho chủ shop." actions={<Button variant="outlined" startIcon={<RefreshOutlinedIcon />} onClick={load}>Tải lại</Button>} />
    {error && <Alert severity="error" sx={{ mt: 3 }} onClose={() => setError("")}>{error}</Alert>}
    <Paper className="admin-data-panel" elevation={0} sx={{ mt: 3, p: 2 }}>
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} spacing={2} sx={{ mb: 2 }}>
        <Stack direction="row" spacing={1} alignItems="center"><StorefrontOutlinedIcon color="primary" /><Typography fontWeight={800}>{shops.length} hồ sơ</Typography></Stack>
        <TextField select size="small" label="Trạng thái" value={status} onChange={(event) => setStatus(event.target.value)} sx={{ minWidth: 170 }}>
          <MenuItem value="">Tất cả</MenuItem><MenuItem value="PENDING">Chờ duyệt</MenuItem><MenuItem value="APPROVED">Đã duyệt</MenuItem><MenuItem value="REJECTED">Từ chối</MenuItem><MenuItem value="SUSPENDED">Tạm ngưng</MenuItem>
        </TextField>
      </Stack>
      {loading ? <Box sx={{ minHeight: 220, display: "grid", placeItems: "center" }}><CircularProgress /></Box> : <Table size="small"><TableHead><TableRow><TableCell>Shop</TableCell><TableCell>Liên hệ</TableCell><TableCell>Địa chỉ</TableCell><TableCell>Ngày gửi</TableCell><TableCell>Trạng thái</TableCell><TableCell align="right">Thao tác</TableCell></TableRow></TableHead><TableBody>
        {shops.map((shop) => <TableRow key={shop.id}><TableCell><Typography fontWeight={800}>{shop.shopName}</Typography><Typography variant="caption" color="text.secondary">{shop.description || "Chưa có mô tả"}</Typography></TableCell><TableCell>{shop.phone}</TableCell><TableCell>{shop.address}, {shop.city}</TableCell><TableCell>{shop.createdAt ? new Date(shop.createdAt).toLocaleString("vi-VN") : "—"}</TableCell><TableCell><Chip label={shop.status} color={statusTone[shop.status] || "default"} size="small" /></TableCell><TableCell align="right">{shop.status === "PENDING" && <Stack direction="row" justifyContent="flex-end" spacing={1}><Button size="small" color="success" startIcon={<CheckCircleOutlineOutlinedIcon />} onClick={() => openReview(shop, "APPROVE")}>Duyệt</Button><Button size="small" color="error" startIcon={<DoNotDisturbOnOutlinedIcon />} onClick={() => openReview(shop, "REJECT")}>Từ chối</Button></Stack>}</TableCell></TableRow>)}
        {!shops.length && <TableRow><TableCell colSpan={6} align="center" sx={{ py: 6 }}><Typography color="text.secondary">Không có hồ sơ phù hợp.</Typography></TableCell></TableRow>}
      </TableBody></Table>}
    </Paper>
    <Dialog open={Boolean(review)} onClose={closeReview} fullWidth maxWidth="sm"><DialogTitle>{reviewingReject ? "Từ chối hồ sơ shop" : "Duyệt hồ sơ shop"}</DialogTitle><DialogContent><Typography sx={{ mb: 2 }}>Shop: <b>{review?.shop.shopName}</b></Typography><TextField autoFocus fullWidth required={reviewingReject} label={reviewingReject ? "Lý do từ chối" : "Ghi chú (không bắt buộc)"} multiline minRows={3} value={note} onChange={(event) => setNote(event.target.value)} inputProps={{ maxLength: 1000 }} /></DialogContent><DialogActions><Button onClick={closeReview}>Hủy</Button><Button variant="contained" color={reviewingReject ? "error" : "success"} disabled={submitting} onClick={submitReview}>{submitting ? "Đang lưu..." : reviewingReject ? "Xác nhận từ chối" : "Xác nhận duyệt"}</Button></DialogActions></Dialog>
  </MainLayout>;
}
