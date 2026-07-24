import {
  Alert,
  Box,
  Button,
  Checkbox,
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
  Pagination,
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
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import DoNotDisturbOnOutlinedIcon from "@mui/icons-material/DoNotDisturbOnOutlined";
import MoreHorizOutlinedIcon from "@mui/icons-material/MoreHorizOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { useEffect, useState } from "react";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getCategories } from "../services/categoryService";
import { getAdminProducts, reviewProduct } from "../services/productService";
import { getSellerShopForAdmin } from "../services/sellerService";

const formatPrice = (value) => {
  if (value === null || value === undefined) return "-";

  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value));
};

const getStockState = (quantity) => {
  if (quantity === null || quantity === undefined) {
    return {
      label: "Unknown",
      color: "default",
      helper: "-",
      tone: "unknown",
    };
  }

  if (Number(quantity) <= 0) {
    return {
      label: "Out of stock",
      color: "error",
      helper: "0 left",
      tone: "out",
    };
  }

  if (Number(quantity) <= 5) {
    return {
      label: "Restock",
      color: "warning",
      helper: `${quantity} left`,
      tone: "restock",
    };
  }

  return {
    label: "In stock",
    color: "success",
    helper: `${quantity} left`,
    tone: "in",
  };
};

const getPrimaryImage = (images = []) => {
  if (!Array.isArray(images) || images.length === 0) return "";

  return images.find((image) => image.isPrimary)?.url || images[0]?.url || "";
};

const getSalesCount = (product) =>
  Number(product.sales ?? product.soldQuantity ?? product.sold ?? 0);

const getRevenue = (product) => Number(product.price || 0) * getSalesCount(product);

const getRating = (product) => Number(product.rating ?? 5).toFixed(1);

const defaultFilters = {
  name: "",
  categoryId: "",
  status: "",
  inStock: "",
  minPrice: "",
  maxPrice: "",
};

export default function Products() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    currentPage: 1,
    pageSize: 10,
    totalPages: 0,
    totalElements: 0,
  });
  const [filters, setFilters] = useState(defaultFilters);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [selectedIds, setSelectedIds] = useState([]);
  const [reviewTarget, setReviewTarget] = useState(null);
  const [reviewAction, setReviewAction] = useState("");
  const [reviewNote, setReviewNote] = useState("");
  const [reviewing, setReviewing] = useState(false);
  const [shopDetail, setShopDetail] = useState(null);
  const [shopLoading, setShopLoading] = useState(false);

  const allVisibleSelected =
    products.length > 0 && selectedIds.length === products.length;
  const someVisibleSelected =
    selectedIds.length > 0 && selectedIds.length < products.length;

  const loadProducts = async (page = pageInfo.currentPage) => {
    setLoading(true);
    setErrorMessage("");

    try {
      const data = await getAdminProducts({
        page,
        size: pageInfo.pageSize,
        ...filters,
      });
      setProducts(data.content || []);
      setSelectedIds([]);
      setPageInfo({
        currentPage: data.currentPage || page,
        pageSize: data.pageSize || pageInfo.pageSize,
        totalPages: data.totalPages || 0,
        totalElements: data.totalElements || 0,
      });
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not load products.",
      );
    } finally {
      setLoading(false);
    }
  };

  const loadCategories = async () => {
    try {
      setCategories(await getCategories());
    } catch {
      setCategories([]);
    }
  };

  useEffect(() => {
    loadCategories();
    loadProducts(1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleFilterChange = (field) => (event) => {
    setFilters((current) => ({ ...current, [field]: event.target.value }));
  };

  const handleSearch = () => {
    loadProducts(1);
  };

  const handleReset = () => {
    setFilters(defaultFilters);
    setTimeout(() => loadProducts(1), 0);
  };

  const toggleProduct = (productId) => {
    setSelectedIds((current) =>
      current.includes(productId)
        ? current.filter((id) => id !== productId)
        : [...current, productId],
    );
  };

  const toggleAllVisible = () => {
    setSelectedIds(allVisibleSelected ? [] : products.map((product) => product.id));
  };

  const openReview = (product, action) => {
    setReviewTarget(product);
    setReviewAction(action);
    setReviewNote("");
  };

  const closeReview = () => {
    if (!reviewing) setReviewTarget(null);
  };

  const submitReview = async () => {
    if (!reviewTarget) return;
    if (reviewAction === "REJECT" && !reviewNote.trim()) {
      setErrorMessage("Nhập lý do từ chối để seller có thể chỉnh sửa sản phẩm.");
      return;
    }
    setReviewing(true);
    try {
      await reviewProduct(reviewTarget.id, { action: reviewAction, note: reviewNote.trim() || null });
      setReviewTarget(null);
      await loadProducts(pageInfo.currentPage);
    } catch (error) {
      setErrorMessage(error.response?.data?.message || "Không thể duyệt sản phẩm.");
    } finally {
      setReviewing(false);
    }
  };

  const openShopDetail = async (product) => {
    if (!product.shopId) return;
    setShopDetail({ loading: true });
    setShopLoading(true);
    try {
      const shop = await getSellerShopForAdmin(product.shopId);
      setShopDetail(shop);
    } catch (error) {
      const message = error.response?.data?.message || "Không thể tải thông tin shop.";
      setShopDetail({ error: message });
    } finally {
      setShopLoading(false);
    }
  };

  const closeShopDetail = () => {
    if (!shopLoading) {
      setShopDetail(null);
    }
  };

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Store service"
        title="Products"
        description="Review marketplace catalog data. Sellers create and manage their own products."
        actions={
          <>
          <Button
            variant="outlined"
            startIcon={<RefreshOutlinedIcon />}
            onClick={() => loadProducts(pageInfo.currentPage)}
          >
            Refresh
          </Button>
          </>
        }
      />

      <Paper
        className="admin-filter-panel"
        elevation={0}
        sx={{ mt: 3, p: 2, border: "1px solid", borderColor: "divider" }}
      >
        <Box className="product-filter-grid">
          <Box>
            <TextField
              label="Name"
              value={filters.name}
              onChange={handleFilterChange("name")}
              fullWidth
            />
          </Box>
          <Box>
            <FormControl fullWidth>
              <InputLabel>Category</InputLabel>
              <Select
                label="Category"
                value={filters.categoryId}
                onChange={handleFilterChange("categoryId")}
              >
                <MenuItem value="">All</MenuItem>
                {categories.map((category) => (
                  <MenuItem key={category.id} value={category.id}>
                    {category.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
          <Box>
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={filters.status}
                onChange={handleFilterChange("status")}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="DRAFT">Draft</MenuItem>
                <MenuItem value="PENDING_APPROVAL">Pending approval</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="REJECTED">Rejected</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
              </Select>
            </FormControl>
          </Box>
          <Box>
            <FormControl fullWidth>
              <InputLabel>Stock</InputLabel>
              <Select
                label="Stock"
                value={filters.inStock}
                onChange={handleFilterChange("inStock")}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="true">In stock</MenuItem>
                <MenuItem value="false">Out of stock</MenuItem>
              </Select>
            </FormControl>
          </Box>
          <Box>
            <TextField
              label="Min"
              type="number"
              value={filters.minPrice}
              onChange={handleFilterChange("minPrice")}
              fullWidth
            />
          </Box>
          <Box>
            <TextField
              label="Max"
              type="number"
              value={filters.maxPrice}
              onChange={handleFilterChange("maxPrice")}
              fullWidth
            />
          </Box>
          <Box className="product-filter-actions">
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button variant="text" onClick={handleReset}>
                Reset
              </Button>
              <Button
                variant="contained"
                startIcon={<SearchOutlinedIcon />}
                onClick={handleSearch}
              >
                Apply filters
              </Button>
            </Stack>
          </Box>
        </Box>
      </Paper>

      <Paper
        className="admin-data-panel"
        elevation={0}
        sx={{
          mt: 3,
          border: "1px solid",
          borderColor: "divider",
          overflow: "hidden",
        }}
      >
        {errorMessage && (
          <Alert severity="error" sx={{ m: 2 }}>
            {errorMessage}
          </Alert>
        )}

        {loading ? (
          <Stack alignItems="center" justifyContent="center" sx={{ py: 8 }}>
            <CircularProgress />
          </Stack>
        ) : products.length === 0 ? (
          <Box sx={{ p: 4 }}>
            <Typography fontWeight={800}>No products found</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Adjust filters to review the marketplace catalog.
            </Typography>
          </Box>
        ) : (
          <>
            <TableContainer className="inventory-table-wrap">
              <Table className="inventory-grid-table">
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox">
                      <Checkbox
                        size="small"
                        checked={allVisibleSelected}
                        indeterminate={someVisibleSelected}
                        onChange={toggleAllVisible}
                      />
                    </TableCell>
                    <TableCell>Product</TableCell>
                    <TableCell>Price</TableCell>
                    <TableCell>Sales</TableCell>
                    <TableCell>Revenue</TableCell>
                    <TableCell>Stock</TableCell>
                    <TableCell>Availability</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Rating</TableCell>
                    <TableCell align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                {products.map((product) => {
                  const stock = getStockState(product.quantity);
                  const imageUrl = getPrimaryImage(product.images);
                  const selected = selectedIds.includes(product.id);

                  return (
                      <TableRow
                        key={product.id}
                        hover
                        selected={selected}
                        className={selected ? "product-row-selected" : ""}
                      >
                        <TableCell padding="checkbox">
                          <Checkbox
                            size="small"
                            checked={selected}
                            onChange={() => toggleProduct(product.id)}
                          />
                        </TableCell>
                        <TableCell>
                          <Stack direction="row" spacing={1.5} alignItems="center">
                            <Box className="product-thumb">
                              {imageUrl ? (
                                <Box
                                  component="img"
                                  src={imageUrl}
                                  alt={product.name}
                                  className="product-thumb-img"
                                />
                              ) : (
                                <Typography className="product-thumb-fallback">
                                  {(product.name || "?").slice(0, 1)}
                                </Typography>
                              )}
                            </Box>
                            <Box sx={{ minWidth: 0 }}>
                              <Button
                                variant="text"
                                size="small"
                                disabled={!product.shopId}
                                onClick={() => openShopDetail(product)}
                                sx={{ p: 0, minWidth: 0, fontWeight: 800, textTransform: "none", justifyContent: "flex-start" }}
                              >
                                {product.name}
                              </Button>
                              <Typography className="table-secondary" noWrap>
                                {product.slug || product.description || "No description"}
                              </Typography>
                            </Box>
                          </Stack>
                        </TableCell>
                        <TableCell>
                          <Typography className="price-plain">
                            {formatPrice(product.price)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography className="table-primary">
                            {getSalesCount(product)} pcs
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography className="price-plain">
                            {formatPrice(getRevenue(product))}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography className="table-primary">
                            {product.quantity ?? "-"}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={stock.label}
                            className={`stock-pill stock-${stock.tone}`}
                          />
                        </TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={product.status || "UNKNOWN"}
                            color={
                              product.status === "ACTIVE" ? "success"
                                : product.status === "PENDING_APPROVAL" ? "warning"
                                  : product.status === "REJECTED" ? "error" : "default"
                            }
                          />
                        </TableCell>
                        <TableCell>
                          <Stack direction="row" spacing={0.5} alignItems="center">
                            <StarRoundedIcon className="rating-star" />
                            <Typography className="table-primary">
                              {getRating(product)}
                            </Typography>
                          </Stack>
                        </TableCell>
                        <TableCell align="center">
                          {product.status === "PENDING_APPROVAL" ? (
                            <Stack direction="row" justifyContent="center" spacing={0.5}>
                              <IconButton color="success" size="small" aria-label="Approve product" onClick={() => openReview(product, "APPROVE")}>
                                <CheckCircleOutlineOutlinedIcon fontSize="small" />
                              </IconButton>
                              <IconButton color="error" size="small" aria-label="Reject product" onClick={() => openReview(product, "REJECT")}>
                                <DoNotDisturbOnOutlinedIcon fontSize="small" />
                              </IconButton>
                            </Stack>
                          ) : "—"}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
            <Stack
              direction={{ xs: "column", sm: "row" }}
              alignItems="center"
              justifyContent="space-between"
              spacing={2}
              sx={{ px: 2, py: 2, borderTop: "1px solid", borderColor: "divider" }}
            >
              <Typography variant="body2" color="text.secondary">
                {pageInfo.totalElements} products
              </Typography>
              <Pagination
                page={pageInfo.currentPage}
                count={Math.max(pageInfo.totalPages, 1)}
                color="primary"
                onChange={(_, page) => loadProducts(page)}
              />
            </Stack>
            {selectedIds.length > 0 && (
              <Stack
                direction="row"
                spacing={1}
                alignItems="center"
                className="selection-toolbar"
              >
                <Typography className="selection-count">
                  {selectedIds.length} Selected
                </Typography>
                <Typography variant="body2" color="text.secondary">Product changes belong to the seller. Moderation actions will be added here.</Typography>
                <IconButton size="small">
                  <MoreHorizOutlinedIcon />
                </IconButton>
                <IconButton size="small" onClick={() => setSelectedIds([])}>
                  <CloseOutlinedIcon />
                </IconButton>
              </Stack>
            )}
          </>
        )}
      </Paper>
      <Dialog open={Boolean(reviewTarget)} onClose={closeReview} fullWidth maxWidth="sm">
        <DialogTitle>{reviewAction === "REJECT" ? "Reject product" : "Approve product"}</DialogTitle>
        <DialogContent>
          <Typography sx={{ mb: 2 }}>Product: <b>{reviewTarget?.name}</b></Typography>
          <TextField
            autoFocus
            fullWidth
            required={reviewAction === "REJECT"}
            label={reviewAction === "REJECT" ? "Reason for rejection" : "Review note (optional)"}
            multiline
            minRows={3}
            value={reviewNote}
            onChange={(event) => setReviewNote(event.target.value)}
            inputProps={{ maxLength: 1000 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={closeReview}>Cancel</Button>
          <Button variant="contained" color={reviewAction === "REJECT" ? "error" : "success"} disabled={reviewing} onClick={submitReview}>
            {reviewing ? "Saving..." : reviewAction === "REJECT" ? "Reject" : "Approve"}
          </Button>
        </DialogActions>
      </Dialog>
      <Dialog open={Boolean(shopDetail)} onClose={closeShopDetail} fullWidth maxWidth="sm">
        <DialogTitle>Thông tin shop</DialogTitle>
        <DialogContent>
          {shopLoading ? <Box sx={{ minHeight: 180, display: "grid", placeItems: "center" }}><CircularProgress /></Box> : shopDetail?.error ? <Alert severity="error">{shopDetail.error}</Alert> : <Stack spacing={2.25}>
            <Box>
              <Typography variant="overline" color="text.secondary">Tên shop</Typography>
              <Typography variant="h6" fontWeight={900}>{shopDetail?.shopName}</Typography>
              <Chip sx={{ mt: 0.75 }} size="small" label={shopDetail?.status} color={shopDetail?.status === "APPROVED" ? "success" : "warning"} />
            </Box>
            <Box><Typography variant="caption" color="text.secondary">Mô tả</Typography><Typography>{shopDetail?.description || "Chưa có mô tả"}</Typography></Box>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={3}>
              <Box><Typography variant="caption" color="text.secondary">Điện thoại</Typography><Typography fontWeight={700}>{shopDetail?.phone || "—"}</Typography></Box>
              <Box><Typography variant="caption" color="text.secondary">Thành phố</Typography><Typography fontWeight={700}>{shopDetail?.city || "—"}</Typography></Box>
            </Stack>
            <Box><Typography variant="caption" color="text.secondary">Địa chỉ</Typography><Typography fontWeight={700}>{shopDetail?.address || "—"}</Typography></Box>
            {shopDetail?.reviewNote && <Alert severity="info">Phản hồi duyệt shop: {shopDetail.reviewNote}</Alert>}
          </Stack>}
        </DialogContent>
        <DialogActions><Button onClick={closeShopDetail}>Đóng</Button></DialogActions>
      </Dialog>
    </MainLayout>
  );
}
