import { useEffect, useState } from "react";
import { Alert, Box, Button, CircularProgress, Paper, Stack, TextField, Typography } from "@mui/material";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import { Navigate, useNavigate } from "react-router-dom";
import { createSellerShop, getMySellerShop, resubmitSellerShop, updateMySellerShop } from "../services/sellerService";

const emptyForm = { shopName: "", description: "", phone: "", address: "", city: "" };

const statusCopy = {
  PENDING: { severity: "warning", title: "Hồ sơ đang chờ duyệt", message: "Shop của bạn đã được gửi đến quản trị viên. Bạn sẽ có quyền Seller sau khi được duyệt." },
  REJECTED: { severity: "error", title: "Hồ sơ cần bổ sung", message: "Vui lòng cập nhật thông tin shop theo phản hồi của quản trị viên và gửi lại." },
  SUSPENDED: { severity: "error", title: "Shop đang tạm ngưng", message: "Vui lòng liên hệ quản trị viên để biết thêm chi tiết." },
};

export default function SellerRegister() {
  const navigate = useNavigate();
  const [form, setForm] = useState(emptyForm);
  const [shop, setShop] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    getMySellerShop()
      .then((data) => { setShop(data); setForm((current) => ({ ...current, ...data })); })
      .catch((requestError) => {
        if (requestError.response?.status !== 404) setError(requestError.response?.data?.message || "Không thể tải trạng thái đăng ký seller.");
      })
      .finally(() => setLoading(false));
  }, []);

  const setField = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      const payload = {
        shopName: form.shopName.trim(), description: form.description.trim() || null,
        phone: form.phone.trim(), address: form.address.trim(), city: form.city.trim(),
      };
      const updatedShop = shop?.status === "REJECTED"
        ? await updateMySellerShop(payload)
        : await createSellerShop(payload);
      const submittedShop = shop?.status === "REJECTED" ? await resubmitSellerShop() : updatedShop;
      setShop(submittedShop);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể gửi hồ sơ đăng ký seller.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Box sx={{ minHeight: "100vh", display: "grid", placeItems: "center", bgcolor: "#f6f8fc" }}><CircularProgress /></Box>;
  if (shop?.status === "APPROVED") return <Navigate to="/seller" replace />;
  const notice = shop ? statusCopy[shop.status] : null;

  return <Box sx={{ minHeight: "100vh", bgcolor: "#f6f8fc", py: { xs: 3, md: 7 }, px: 2 }}>
    <Box sx={{ maxWidth: 760, mx: "auto" }}>
      <Button startIcon={<ArrowBackOutlinedIcon />} onClick={() => navigate("/shop")} sx={{ mb: 2 }}>Quay lại cửa hàng</Button>
      <Paper elevation={0} className="admin-data-panel" sx={{ p: { xs: 2.5, md: 4 } }}>
        <Stack spacing={1} sx={{ mb: 3 }}>
          <StorefrontOutlinedIcon color="primary" sx={{ fontSize: 34 }} />
          <Typography component="h1" variant="h4" fontWeight={900}>Đăng ký trở thành seller</Typography>
          <Typography color="text.secondary">Tạo hồ sơ shop để bán hàng. Quản trị viên sẽ xem xét trước khi kích hoạt quyền Seller.</Typography>
        </Stack>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {notice && <Stack spacing={2} sx={{ mb: shop?.status === "REJECTED" ? 3 : 0 }}><Alert severity={notice.severity}><Typography fontWeight={800}>{notice.title}</Typography>{notice.message}</Alert>{shop.reviewNote && <Alert severity="info">Phản hồi: {shop.reviewNote}</Alert>}</Stack>}
        {(!shop || shop.status === "REJECTED") && <Box component="form" onSubmit={submit}><Stack spacing={2.25}>
          <TextField label="Tên shop" value={form.shopName} onChange={setField("shopName")} required inputProps={{ maxLength: 160 }} />
          <TextField label="Mô tả shop" value={form.description} onChange={setField("description")} multiline minRows={3} inputProps={{ maxLength: 2000 }} />
          <TextField label="Số điện thoại" value={form.phone} onChange={setField("phone")} required inputProps={{ maxLength: 30 }} />
          <TextField label="Địa chỉ" value={form.address} onChange={setField("address")} required inputProps={{ maxLength: 500 }} />
          <TextField label="Tỉnh / thành phố" value={form.city} onChange={setField("city")} required inputProps={{ maxLength: 120 }} />
          <Alert severity="info">Hệ thống chưa yêu cầu tải giấy tờ. Bạn có thể bổ sung bước KYC khi backend hỗ trợ lưu trữ tài liệu.</Alert>
          <Button type="submit" variant="contained" size="large" disabled={submitting}>{submitting ? <CircularProgress size={24} color="inherit" /> : shop?.status === "REJECTED" ? "Cập nhật và gửi lại" : "Gửi hồ sơ đăng ký"}</Button>
        </Stack></Box>}
      </Paper>
    </Box>
  </Box>;
}
