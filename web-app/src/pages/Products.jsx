import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  FormControl,
  Grid,
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
  Tooltip,
  Typography,
} from "@mui/material";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { getCategories } from "../services/categoryService";
import { deleteProduct, getProducts } from "../services/productService";
import { formatDateTime } from "../utils/dateTimeUtils";

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
    };
  }

  if (Number(quantity) <= 0) {
    return {
      label: "Out of stock",
      color: "error",
      helper: "0 left",
    };
  }

  return {
    label: "In stock",
    color: "success",
    helper: `${quantity} left`,
  };
};

const defaultFilters = {
  name: "",
  categoryId: "",
  status: "",
  inStock: "",
  minPrice: "",
  maxPrice: "",
};

export default function Products() {
  const navigate = useNavigate();
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

  const loadProducts = async (page = pageInfo.currentPage) => {
    setLoading(true);
    setErrorMessage("");

    try {
      const data = await getProducts({
        page,
        size: pageInfo.pageSize,
        ...filters,
      });
      setProducts(data.content || []);
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

  const handleDelete = async (product) => {
    if (!window.confirm(`Delete product "${product.name}"?`)) return;

    try {
      await deleteProduct(product.id);
      await loadProducts(pageInfo.currentPage);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not delete product.",
      );
    }
  };

  return (
    <MainLayout>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        justifyContent="space-between"
        alignItems={{ xs: "stretch", sm: "center" }}
        spacing={2}
      >
        <Box>
          <Typography variant="h4" fontWeight={900}>
            Products
          </Typography>
          <Typography color="text.secondary">
            View, filter, and manage product catalog data.
          </Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            startIcon={<RefreshOutlinedIcon />}
            onClick={() => loadProducts(pageInfo.currentPage)}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddOutlinedIcon />}
            onClick={() => navigate("/products/new")}
          >
            New product
          </Button>
        </Stack>
      </Stack>

      <Paper
        elevation={0}
        sx={{ mt: 3, p: 2, border: "1px solid", borderColor: "divider" }}
      >
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={3}>
            <TextField
              label="Name"
              value={filters.name}
              onChange={handleFilterChange("name")}
              fullWidth
            />
          </Grid>
          <Grid item xs={12} md={3}>
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
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={filters.status}
                onChange={handleFilterChange("status")}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
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
          </Grid>
          <Grid item xs={6} md={1}>
            <TextField
              label="Min"
              type="number"
              value={filters.minPrice}
              onChange={handleFilterChange("minPrice")}
              fullWidth
            />
          </Grid>
          <Grid item xs={6} md={1}>
            <TextField
              label="Max"
              type="number"
              value={filters.maxPrice}
              onChange={handleFilterChange("maxPrice")}
              fullWidth
            />
          </Grid>
          <Grid item xs={12}>
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
          </Grid>
        </Grid>
      </Paper>

      <Paper
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
              Adjust filters or create a new product.
            </Typography>
          </Box>
        ) : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Slug</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Price</TableCell>
                    <TableCell align="right">Quantity</TableCell>
                    <TableCell>Created At</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {products.map((product) => {
                    const stock = getStockState(product.quantity);

                    return (
                      <TableRow key={product.id} hover>
                        <TableCell>
                          <Typography fontWeight={800}>{product.name}</Typography>
                          <Typography variant="body2" color="text.secondary">
                            {product.description || "No description"}
                          </Typography>
                        </TableCell>
                        <TableCell>{product.slug || "-"}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={product.status || "UNKNOWN"}
                            color={
                              product.status === "ACTIVE" ? "success" : "default"
                            }
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell align="right">
                          {formatPrice(product.price)}
                        </TableCell>
                        <TableCell align="right">
                          <Stack alignItems="flex-end" spacing={0.5}>
                            <Chip
                              size="small"
                              label={stock.label}
                              color={stock.color}
                              variant={stock.color === "error" ? "filled" : "outlined"}
                            />
                            <Typography variant="body2" color="text.secondary">
                              {stock.helper}
                            </Typography>
                          </Stack>
                        </TableCell>
                        <TableCell>{formatDateTime(product.createdAt)}</TableCell>
                        <TableCell align="right">
                          <Tooltip title="Delete">
                            <IconButton
                              color="error"
                              onClick={() => handleDelete(product)}
                            >
                              <DeleteOutlineOutlinedIcon />
                            </IconButton>
                          </Tooltip>
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
          </>
        )}
      </Paper>
    </MainLayout>
  );
}
