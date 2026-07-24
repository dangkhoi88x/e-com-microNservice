import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getMySellerShop, updateMySellerShop } from "../services/sellerService";

const emptyShop = {
  shopName: "",
  description: "",
  phone: "",
  address: "",
  city: "",
};

const statusColor = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "error",
  SUSPENDED: "default",
};

export default function SellerShop() {
  const [shop, setShop] = useState(null);
  const [form, setForm] = useState(emptyShop);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    getMySellerShop()
      .then((data) => {
        setShop(data);
        setForm({
          shopName: data?.shopName || "",
          description: data?.description || "",
          phone: data?.phone || "",
          address: data?.address || "",
          city: data?.city || "",
        });
      })
      .catch((requestError) => {
        setError(requestError.response?.data?.message || "Không thể tải thông tin shop.");
      })
      .finally(() => setLoading(false));
  }, []);

  const change = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const updated = await updateMySellerShop({
        shopName: form.shopName.trim(),
        description: form.description.trim() || null,
        phone: form.phone.trim(),
        address: form.address.trim(),
        city: form.city.trim(),
      });
      setShop(updated);
      setSuccess("Thông tin shop đã được cập nhật.");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể cập nhật thông tin shop.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Seller center"
        title="Thông tin cửa hàng"
        description="Quản lý thông tin công khai và thông tin liên hệ của shop."
      />

      {loading ? (
        <Box sx={{ minHeight: 280, display: "grid", placeItems: "center" }}>
          <CircularProgress />
        </Box>
      ) : (
        <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1fr) 320px" }, gap: 3, mt: 3 }}>
          <Paper component="form" onSubmit={submit} className="admin-data-panel" elevation={0} sx={{ p: { xs: 2.5, md: 4 } }}>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess("")}>{success}</Alert>}
            <Stack spacing={2.25}>
              <TextField label="Tên shop" required value={form.shopName} onChange={change("shopName")} inputProps={{ maxLength: 160 }} />
              <TextField label="Mô tả shop" multiline minRows={4} value={form.description} onChange={change("description")} inputProps={{ maxLength: 2000 }} />
              <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                <TextField fullWidth label="Số điện thoại" required value={form.phone} onChange={change("phone")} inputProps={{ maxLength: 30 }} />
                <TextField fullWidth label="Tỉnh / thành phố" required value={form.city} onChange={change("city")} inputProps={{ maxLength: 120 }} />
              </Stack>
              <TextField label="Địa chỉ" required value={form.address} onChange={change("address")} inputProps={{ maxLength: 500 }} />
              <Box>
                <Button type="submit" variant="contained" size="large" disabled={saving} startIcon={!saving && <SaveOutlinedIcon />}>
                  {saving ? "Đang lưu..." : "Lưu thay đổi"}
                </Button>
              </Box>
            </Stack>
          </Paper>

          <Paper className="admin-data-panel" elevation={0} sx={{ p: 3, alignSelf: "start" }}>
            <Stack spacing={2}>
              <Box sx={{ width: 52, height: 52, borderRadius: 3, bgcolor: "primary.50", color: "primary.main", display: "grid", placeItems: "center" }}>
                <StorefrontOutlinedIcon />
              </Box>
              <Box>
                <Typography variant="overline" color="text.secondary">Trạng thái shop</Typography>
                <Box sx={{ mt: 0.5 }}><Chip label={shop?.status || "UNKNOWN"} color={statusColor[shop?.status] || "default"} /></Box>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Đường dẫn shop</Typography>
                <Typography fontWeight={700}>{shop?.slug || "—"}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Ngày tạo</Typography>
                <Typography fontWeight={700}>{shop?.createdAt ? new Date(shop.createdAt).toLocaleDateString("vi-VN") : "—"}</Typography>
              </Box>
              {shop?.reviewNote && <Alert severity="info">Phản hồi admin: {shop.reviewNote}</Alert>}
            </Stack>
          </Paper>
        </Box>
      )}
    </MainLayout>
  );
}
