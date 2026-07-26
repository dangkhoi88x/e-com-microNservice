import { useCallback, useEffect, useMemo, useState } from "react";
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
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import {
  deleteSellerProduct,
  getMySellerProducts,
  submitSellerProduct,
  updateSellerProductQuantity,
  updateSellerProductStatus,
} from "../services/productService";

const statusTone = {
  DRAFT: "default",
  PENDING_APPROVAL: "warning",
  ACTIVE: "success",
  REJECTED: "error",
  INACTIVE: "default",
};

const getPrimaryImage = (images = []) =>
  images.find((image) => image.isPrimary)?.url || images[0]?.url || "";

const stockState = (quantity) => {
  if (Number(quantity || 0) <= 0) return { label: "Hết hàng", color: "error" };
  if (Number(quantity) <= 5) return { label: `Còn ${quantity}`, color: "warning" };
  return { label: `Còn ${quantity}`, color: "success" };
};

export default function SellerProducts() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [quantityTarget, setQuantityTarget] = useState(null);
  const [nextQuantity, setNextQuantity] = useState("");
  const [updatingQuantity, setUpdatingQuantity] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getMySellerProducts({ page: 1, size: 100 });
      setProducts(data.content || []);
      setError("");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể tải sản phẩm của shop.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const visibleProducts = useMemo(
    () =>
      products.filter((product) => {
        const matchName = product.name?.toLowerCase().includes(query.trim().toLowerCase());
        return matchName && (!status || product.status === status);
      }),
    [products, query, status],
  );

  const submit = async (product) => {
    try {
      await submitSellerProduct(product.id);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể gửi sản phẩm để duyệt.");
    }
  };

  const remove = async (product) => {
    if (!window.confirm(`Xóa sản phẩm “${product.name}”?`)) return;
    try {
      await deleteSellerProduct(product.id);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể xóa sản phẩm này.");
    }
  };

  const openQuantityEditor = (product) => {
    setQuantityTarget(product);
    setNextQuantity(String(product.quantity ?? 0));
  };

  const closeQuantityEditor = () => {
    if (!updatingQuantity) setQuantityTarget(null);
  };

  const updateQuantity = async () => {
    const quantity = Number(nextQuantity);
    if (!Number.isInteger(quantity) || quantity < 0) {
      setError("Số lượng phải là số nguyên lớn hơn hoặc bằng 0.");
      return;
    }

    setUpdatingQuantity(true);
    try {
      await updateSellerProductQuantity(quantityTarget.id, quantity);
      setQuantityTarget(null);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể cập nhật số lượng sản phẩm.");
    } finally {
      setUpdatingQuantity(false);
    }
  };

  const updateStatus = async (product) => {
    const nextStatus = product.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    const nextLabel = nextStatus === "ACTIVE" ? "hiển thị để bán" : "ẩn khỏi cửa hàng";
    if (!window.confirm(`Bạn muốn ${nextLabel} sản phẩm “${product.name}”?`)) return;

    try {
      await updateSellerProductStatus(product.id, nextStatus);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể cập nhật trạng thái sản phẩm.");
    }
  };

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Seller center"
        title="Sản phẩm của shop"
        description="Theo dõi và quản lý toàn bộ catalog, bao gồm sản phẩm đã được duyệt."
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<RefreshOutlinedIcon />} onClick={load}>
              Tải lại
            </Button>
            <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => navigate("/seller/products/new")}>
              Tạo sản phẩm
            </Button>
          </Stack>
        }
      />

      {error && <Alert severity="error" sx={{ mt: 3 }} onClose={() => setError("")}>{error}</Alert>}

      <Paper className="admin-filter-panel" elevation={0} sx={{ mt: 3, p: 2 }}>
        <Stack direction={{ xs: "column", md: "row" }} spacing={2} alignItems={{ md: "center" }}>
          <TextField label="Tìm sản phẩm" value={query} onChange={(event) => setQuery(event.target.value)} sx={{ minWidth: { md: 300 }, flex: 1 }} />
          <FormControl sx={{ minWidth: 210 }}>
            <InputLabel>Trạng thái</InputLabel>
            <Select label="Trạng thái" value={status} onChange={(event) => setStatus(event.target.value)}>
              <MenuItem value="">Tất cả</MenuItem>
              <MenuItem value="DRAFT">Bản nháp</MenuItem>
              <MenuItem value="PENDING_APPROVAL">Chờ duyệt</MenuItem>
              <MenuItem value="ACTIVE">Đang bán</MenuItem>
              <MenuItem value="REJECTED">Bị từ chối</MenuItem>
              <MenuItem value="INACTIVE">Đã ẩn</MenuItem>
            </Select>
          </FormControl>
        </Stack>
      </Paper>

      <Paper className="admin-data-panel" elevation={0} sx={{ mt: 3, overflow: "hidden" }}>
        {loading ? (
          <Box sx={{ minHeight: 260, display: "grid", placeItems: "center" }}><CircularProgress /></Box>
        ) : (
          <>
            <TableContainer>
              <Table className="inventory-grid-table" sx={{ minWidth: 840 }}>
                <TableHead><TableRow><TableCell>Sản phẩm</TableCell><TableCell>Giá</TableCell><TableCell>Kho</TableCell><TableCell>Trạng thái</TableCell><TableCell>Phản hồi duyệt</TableCell><TableCell align="right">Thao tác</TableCell></TableRow></TableHead>
                <TableBody>
                  {visibleProducts.map((product) => {
                    const imageUrl = getPrimaryImage(product.images || []);
                    const stock = stockState(product.quantity);
                    const editable = ["DRAFT", "REJECTED"].includes(product.status);
                    const quantityEditable = product.status !== "PENDING_APPROVAL";

                    return (
                      <TableRow key={product.id} hover>
                        <TableCell>
                          <Stack direction="row" spacing={1.5} alignItems="center">
                            <Box className="product-thumb">
                              {imageUrl ? <Box component="img" src={imageUrl} alt={product.name} className="product-thumb-img" /> : <Typography className="product-thumb-fallback">{(product.name || "?").slice(0, 1)}</Typography>}
                            </Box>
                            <Box><Typography className="table-primary">{product.name}</Typography><Typography className="table-secondary">{product.slug || product.categoryName || "—"}</Typography></Box>
                          </Stack>
                        </TableCell>
                        <TableCell><Typography className="price-plain">{Number(product.price || 0).toLocaleString("vi-VN")} đ</Typography></TableCell>
                        <TableCell><Chip label={stock.label} size="small" color={stock.color} /></TableCell>
                        <TableCell><Chip label={product.status} size="small" color={statusTone[product.status] || "default"} /></TableCell>
                        <TableCell><Typography variant="body2" color={product.moderationNote ? "error" : "text.secondary"}>{product.moderationNote || "—"}</Typography></TableCell>
                        <TableCell align="right">
                          <Stack direction="row" justifyContent="flex-end" spacing={0.5} alignItems="center">
                            {quantityEditable && <IconButton size="small" color="primary" aria-label="Sửa số lượng" title="Sửa số lượng" onClick={() => openQuantityEditor(product)}><EditOutlinedIcon fontSize="small" /></IconButton>}
                            {editable && <Button size="small" onClick={() => navigate(`/seller/products/${product.id}/edit`)}>Chỉnh sửa</Button>}
                            {["ACTIVE", "INACTIVE"].includes(product.status) && <Button size="small" onClick={() => updateStatus(product)}>{product.status === "ACTIVE" ? "Ẩn sản phẩm" : "Đăng bán"}</Button>}
                            {editable && <Button size="small" startIcon={<SendOutlinedIcon />} onClick={() => submit(product)}>Gửi duyệt</Button>}
                            {editable && <IconButton size="small" color="error" aria-label="Xóa sản phẩm" onClick={() => remove(product)}><DeleteOutlineOutlinedIcon fontSize="small" /></IconButton>}
                            {product.status === "PENDING_APPROVAL" && <Typography variant="caption" color="text.secondary">Đang chờ admin</Typography>}
                            {product.status === "ACTIVE" && <Typography variant="caption" color="success.main">Đang hiển thị</Typography>}
                          </Stack>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                  {!visibleProducts.length && <TableRow><TableCell colSpan={6} align="center" sx={{ py: 7 }}><Typography fontWeight={700}>Không có sản phẩm phù hợp.</Typography></TableCell></TableRow>}
                </TableBody>
              </Table>
            </TableContainer>
            <Box sx={{ px: 2, py: 1.5, borderTop: "1px solid", borderColor: "divider" }}><Typography variant="body2" color="text.secondary">{visibleProducts.length} / {products.length} sản phẩm</Typography></Box>
          </>
        )}
      </Paper>

      <Dialog open={Boolean(quantityTarget)} onClose={closeQuantityEditor} fullWidth maxWidth="xs">
        <DialogTitle>Cập nhật số lượng</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{quantityTarget?.name}</Typography>
          <TextField autoFocus fullWidth label="Số lượng sản phẩm" type="number" value={nextQuantity} onChange={(event) => setNextQuantity(event.target.value)} inputProps={{ min: 0, step: 1 }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={closeQuantityEditor} disabled={updatingQuantity}>Hủy</Button>
          <Button variant="contained" onClick={updateQuantity} disabled={updatingQuantity}>{updatingQuantity ? "Đang lưu..." : "Cập nhật"}</Button>
        </DialogActions>
      </Dialog>
    </MainLayout>
  );
}
