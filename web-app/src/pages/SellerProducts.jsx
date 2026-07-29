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
import CloudUploadOutlinedIcon from "@mui/icons-material/CloudUploadOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "../components/admin";
import { VariantAttributeFields } from "../components/products/ProductOptionsEditor";
import MainLayout from "../layouts/MainLayout";
import { deleteMedia, uploadProductImage } from "../services/mediaService";
import {
  addSellerProductVariant,
  deleteSellerProduct,
  getSellerProductById,
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
  const [variantProduct, setVariantProduct] = useState(null);
  const [variant, setVariant] = useState({ sku: "", attributes: {}, price: "", quantity: "", imageFile: null, imagePreviewUrl: "" });
  const [loadingVariantProduct, setLoadingVariantProduct] = useState(false);
  const [addingVariant, setAddingVariant] = useState(false);

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

  const openVariantDialog = async (product) => {
    setLoadingVariantProduct(true);
    setError("");
    try {
      const detail = await getSellerProductById(product.id);
      if (detail.status !== "ACTIVE") {
        setError("Chỉ có thể thêm variant cho sản phẩm đang bán.");
        return;
      }
      setVariantProduct(detail);
      setVariant({ sku: "", attributes: {}, price: String(detail.price ?? ""), quantity: "0", imageFile: null, imagePreviewUrl: "" });
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể tải thông tin sản phẩm.");
    } finally {
      setLoadingVariantProduct(false);
    }
  };

  const closeVariantDialog = () => {
    if (!addingVariant) {
      if (variant.imagePreviewUrl) URL.revokeObjectURL(variant.imagePreviewUrl);
      setVariantProduct(null);
    }
  };

  const updateVariantAttribute = (name, value) => {
    setVariant((current) => ({ ...current, attributes: { ...current.attributes, [name]: value } }));
  };

  const selectVariantImage = (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    const allowedImageTypes = new Set(["image/jpeg", "image/png", "image/webp"]);
    const maxImageBytes = 6 * 1024 * 1024;
    if (!allowedImageTypes.has(file.type) || file.size > maxImageBytes) {
      setError("Ảnh variant phải là JPEG, PNG hoặc WebP và không quá 6 MB.");
      return;
    }
    setVariant((current) => {
      if (current.imagePreviewUrl) URL.revokeObjectURL(current.imagePreviewUrl);
      return { ...current, imageFile: file, imagePreviewUrl: URL.createObjectURL(file) };
    });
  };

  const removeVariantImage = () => {
    setVariant((current) => {
      if (current.imagePreviewUrl) URL.revokeObjectURL(current.imagePreviewUrl);
      return { ...current, imageFile: null, imagePreviewUrl: "" };
    });
  };

  const addVariant = async () => {
    const price = Number(variant.price);
    const quantity = Number(variant.quantity);
    if (!variant.sku.trim()) {
      setError("SKU variant không được để trống.");
      return;
    }
    if (!Number.isFinite(price) || price <= 0) {
      setError("Giá variant phải lớn hơn 0.");
      return;
    }
    if (!Number.isInteger(quantity) || quantity < 0) {
      setError("Số lượng variant phải là số nguyên lớn hơn hoặc bằng 0.");
      return;
    }
    const missingOption = (variantProduct.options || []).find((option) => option.required && !variant.attributes[option.name]);
    if (missingOption) {
      setError(`Hãy chọn ${missingOption.displayName || missingOption.name} cho variant.`);
      return;
    }

    setAddingVariant(true);
    let uploadedMedia;
    try {
      if (variant.imageFile) uploadedMedia = await uploadProductImage(variant.imageFile);
      await addSellerProductVariant(variantProduct.id, {
        sku: variant.sku.trim(),
        attributes: variant.attributes,
        price,
        quantity,
        imageUrl: uploadedMedia?.contentUrl || null,
      });
      if (variant.imagePreviewUrl) URL.revokeObjectURL(variant.imagePreviewUrl);
      setVariantProduct(null);
      await load();
    } catch (requestError) {
      if (uploadedMedia) deleteMedia(uploadedMedia.id).catch(() => {});
      setError(requestError.response?.data?.message || "Không thể thêm variant.");
    } finally {
      setAddingVariant(false);
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
                            {product.status === "ACTIVE" && <Button size="small" startIcon={<AddOutlinedIcon />} onClick={() => openVariantDialog(product)} disabled={loadingVariantProduct}>Thêm variant</Button>}
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

      <Dialog open={Boolean(variantProduct)} onClose={closeVariantDialog} fullWidth maxWidth="sm">
        <DialogTitle>Thêm variant</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {variantProduct?.name}. Variant mới sẽ được đăng bán ngay và không thay đổi thông tin sản phẩm hiện có.
          </Typography>
          <Stack spacing={2} sx={{ pt: 0.5 }}>
            <TextField autoFocus required fullWidth label="SKU variant" value={variant.sku} onChange={(event) => setVariant((current) => ({ ...current, sku: event.target.value }))} />
            {(variantProduct?.options || []).length > 0 && <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} flexWrap="wrap">
              <VariantAttributeFields options={variantProduct.options} attributes={variant.attributes} onChange={updateVariantAttribute} />
            </Stack>}
            <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
              <TextField required fullWidth label="Giá variant" type="number" value={variant.price} onChange={(event) => setVariant((current) => ({ ...current, price: event.target.value }))} inputProps={{ min: 1, step: 1000 }} />
              <TextField required fullWidth label="Số lượng" type="number" value={variant.quantity} onChange={(event) => setVariant((current) => ({ ...current, quantity: event.target.value }))} inputProps={{ min: 0, step: 1 }} />
            </Stack>
            <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
              {variant.imagePreviewUrl && <Box component="img" src={variant.imagePreviewUrl} alt="Xem trước ảnh variant" sx={{ width: 72, height: 72, objectFit: "cover", borderRadius: 1, border: "1px solid", borderColor: "divider" }} />}
              <Button component="label" variant="outlined" startIcon={<CloudUploadOutlinedIcon />}>
                {variant.imageFile ? "Đổi ảnh variant" : "Tải ảnh variant"}
                <input hidden type="file" accept="image/jpeg,image/png,image/webp" onChange={selectVariantImage} />
              </Button>
              {variant.imageFile && <Button color="error" onClick={removeVariantImage}>Bỏ ảnh</Button>}
              <Typography variant="caption" color="text.secondary">JPEG, PNG hoặc WebP · tối đa 6 MB</Typography>
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeVariantDialog} disabled={addingVariant}>Hủy</Button>
          <Button variant="contained" onClick={addVariant} disabled={addingVariant}>{addingVariant ? "Đang thêm..." : "Thêm variant"}</Button>
        </DialogActions>
      </Dialog>
    </MainLayout>
  );
}
