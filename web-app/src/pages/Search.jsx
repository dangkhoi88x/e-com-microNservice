import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  FormControl,
  InputLabel,
  LinearProgress,
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
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import InsightsOutlinedIcon from "@mui/icons-material/InsightsOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import PaymentsOutlinedIcon from "@mui/icons-material/PaymentsOutlined";
import { useEffect, useState } from "react";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import {
  getProductAggregations,
  searchProducts,
} from "../services/productService";
import { formatDateTime } from "../utils/dateTimeUtils";

const formatPrice = (value) => {
  if (value === null || value === undefined) return "-";

  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value));
};

const stockChipProps = (inStock) => {
  if (inStock === true) {
    return {
      label: "In stock",
      color: "success",
      variant: "outlined",
    };
  }

  if (inStock === false) {
    return {
      label: "Out of stock",
      color: "error",
      variant: "filled",
    };
  }

  return {
    label: "Unknown",
    color: "default",
    variant: "outlined",
  };
};

const getResultInitial = (name = "") => name.trim().slice(0, 1) || "?";

const defaultFilters = {
  name: "",
  description: "",
  categoryId: "",
  status: "",
  inStock: "",
  minPrice: "",
  maxPrice: "",
};

export default function Search() {
  const [filters, setFilters] = useState(defaultFilters);
  const [products, setProducts] = useState([]);
  const [aggregations, setAggregations] = useState({
    categories: [],
    priceStats: { count: 0 },
    priceRanges: [],
  });
  const [pageInfo, setPageInfo] = useState({
    currentPage: 1,
    pageSize: 20,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  const loadSearch = async (page = pageInfo.currentPage) => {
    setLoading(true);
    setErrorMessage("");

    try {
      const params = {
        page,
        size: pageInfo.pageSize,
        ...filters,
      };

      const [searchData, aggregationData] = await Promise.all([
        searchProducts(params),
        getProductAggregations(filters),
      ]);

      setProducts(searchData.content || []);
      setPageInfo({
        currentPage: searchData.currentPage || page,
        pageSize: searchData.pageSize || pageInfo.pageSize,
        totalPages: searchData.totalPages || 0,
        totalElements: searchData.totalElements || 0,
      });
      setAggregations(aggregationData);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not search products.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSearch(1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setField = (field) => (event) => {
    setFilters((current) => ({
      ...current,
      [field]: event.target.value,
    }));
  };

  const handleReset = () => {
    setFilters(defaultFilters);
    setTimeout(() => loadSearch(1), 0);
  };

  const priceStats = aggregations.priceStats || {};
  const categories = aggregations.categories || [];
  const priceRanges = aggregations.priceRanges || [];
  const largestCategoryCount = Math.max(...categories.map((item) => item.count || 0), 1);
  const largestRangeCount = Math.max(...priceRanges.map((item) => item.count || 0), 1);

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Discovery"
        title="Search"
        description="Query Elasticsearch with filters, pagination, and aggregations."
      />

      <Stack spacing={3} sx={{ mt: 2 }}>
        <Paper
          className="search-filter-card"
          elevation={0}
          sx={{ p: 2, border: "1px solid", borderColor: "divider" }}
        >
            <Stack direction="row" spacing={1.2} alignItems="center">
              <Box className="insight-mark">
                <SearchOutlinedIcon fontSize="small" />
              </Box>
              <Box>
                <Typography fontWeight={900}>Filters</Typography>
                <Typography variant="body2" color="text.secondary">
                  Narrow indexed catalog records
                </Typography>
              </Box>
            </Stack>
            <Box className="search-filter-grid">
              <TextField
                label="Name"
                value={filters.name}
                onChange={setField("name")}
                fullWidth
              />
              <TextField
                label="Description"
                value={filters.description}
                onChange={setField("description")}
                fullWidth
              />
              <FormControl fullWidth className="search-filter-compact">
                <InputLabel>Status</InputLabel>
                <Select
                  label="Status"
                  value={filters.status}
                  onChange={setField("status")}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="INACTIVE">Inactive</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth className="search-filter-compact">
                <InputLabel>Stock</InputLabel>
                <Select
                  label="Stock"
                  value={filters.inStock}
                  onChange={setField("inStock")}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="true">In stock</MenuItem>
                  <MenuItem value="false">Out of stock</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="Min price"
                type="number"
                value={filters.minPrice}
                onChange={setField("minPrice")}
                fullWidth
              />
              <TextField
                label="Max price"
                type="number"
                value={filters.maxPrice}
                onChange={setField("maxPrice")}
                fullWidth
              />
              <Stack direction="row" spacing={1} className="search-filter-actions">
                <Button fullWidth onClick={handleReset}>
                  Reset
                </Button>
                <Button
                  fullWidth
                  variant="contained"
                  startIcon={<SearchOutlinedIcon />}
                  onClick={() => loadSearch(1)}
                >
                  Search
                </Button>
              </Stack>
            </Box>

            <Divider sx={{ my: 2 }} />

            <Stack
              direction={{ xs: "column", sm: "row" }}
              alignItems={{ xs: "flex-start", sm: "center" }}
              justifyContent="space-between"
              spacing={1}
              className="aggregation-heading"
            >
              <Stack direction="row" spacing={1.25} alignItems="center" className="section-library-heading">
                <Box className="section-heading-icon section-heading-icon-green"><InsightsOutlinedIcon /></Box>
                <Box>
                <Typography className="section-heading-title">Tổng quan dữ liệu</Typography>
                <Typography className="section-heading-description">
                  Phân bổ sản phẩm theo giá và danh mục của kết quả hiện tại.
                </Typography>
                </Box>
              </Stack>
              <Chip
                icon={<InsightsOutlinedIcon />}
                label={`${priceStats.count || 0} sản phẩm`}
                className="aggregation-total-chip"
              />
            </Stack>
            {loading ? (
              <LinearProgress sx={{ mt: 2 }} />
            ) : (
              <Box className="search-aggregation-grid">
                <Box className="aggregation-card aggregation-price-card">
                  <Box className="aggregation-card-heading">
                    <Box className="aggregation-icon aggregation-icon-price">
                      <PaymentsOutlinedIcon />
                    </Box>
                    <Box>
                      <Typography className="aggregation-label">Khoảng giá</Typography>
                      <Typography className="aggregation-caption">Toàn bộ kết quả</Typography>
                    </Box>
                  </Box>
                  <Typography className="aggregation-price-range">
                    {formatPrice(priceStats.min)} - {formatPrice(priceStats.max)}
                  </Typography>
                  <Typography className="aggregation-stat-line">
                    Trung bình <strong>{formatPrice(priceStats.avg)}</strong>
                  </Typography>
                </Box>

                <Box className="aggregation-card">
                  <Box className="aggregation-card-heading">
                    <Box className="aggregation-icon aggregation-icon-category">
                      <CategoryOutlinedIcon />
                    </Box>
                    <Box>
                      <Typography className="aggregation-label">Danh mục</Typography>
                      <Typography className="aggregation-caption">Nhóm sản phẩm nổi bật</Typography>
                    </Box>
                  </Box>
                  <Stack spacing={1.25} className="aggregation-list">
                    {categories.length ? categories.map((category) => (
                      <Box
                        key={category.name}
                        className="aggregation-list-item"
                      >
                        <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1}>
                          <Typography className="aggregation-item-name">{category.name}</Typography>
                          <Chip size="small" label={category.count} className="aggregation-count-chip" />
                        </Stack>
                        <Box className="aggregation-meter">
                          <Box sx={{ width: `${((category.count || 0) / largestCategoryCount) * 100}%` }} />
                        </Box>
                      </Box>
                    )) : <Typography className="aggregation-empty">Chưa có dữ liệu danh mục.</Typography>}
                  </Stack>
                </Box>

                <Box className="aggregation-card">
                  <Box className="aggregation-card-heading">
                    <Box className="aggregation-icon aggregation-icon-range">
                      <LocalOfferOutlinedIcon />
                    </Box>
                    <Box>
                      <Typography className="aggregation-label">Phân khúc giá</Typography>
                      <Typography className="aggregation-caption">Mật độ theo khoảng giá</Typography>
                    </Box>
                  </Box>
                  <Stack spacing={1.25} className="aggregation-list">
                    {priceRanges.length ? priceRanges.map((range) => (
                      <Box
                        key={range.range}
                        className="aggregation-list-item"
                      >
                        <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1}>
                          <Typography className="aggregation-item-name">{range.range}</Typography>
                          <Chip size="small" label={range.count} className="aggregation-count-chip" />
                        </Stack>
                        <Box className="aggregation-meter aggregation-range-meter">
                          <Box sx={{ width: `${((range.count || 0) / largestRangeCount) * 100}%` }} />
                        </Box>
                      </Box>
                    )) : <Typography className="aggregation-empty">Chưa có dữ liệu phân khúc.</Typography>}
                  </Stack>
                </Box>
              </Box>
            )}
        </Paper>

        <Paper
          className="admin-data-panel"
          elevation={0}
          sx={{ border: "1px solid", borderColor: "divider", overflow: "hidden" }}
        >
            <Stack
              direction={{ xs: "column", sm: "row" }}
              justifyContent="space-between"
              alignItems={{ xs: "stretch", sm: "center" }}
              spacing={2}
              className="panel-summary"
            >
              <Box>
                <Typography fontWeight={900}>Search results</Typography>
                <Typography variant="body2" color="text.secondary">
                  {pageInfo.totalElements} indexed products matched
                </Typography>
              </Box>
              <Chip
                label={`${products.length} shown`}
                color="primary"
                variant="outlined"
              />
            </Stack>
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
                <Typography fontWeight={800}>No matching products</Typography>
                <Typography color="text.secondary" sx={{ mt: 1 }}>
                  Change filters or wait for Kafka to index new product events.
                </Typography>
              </Box>
            ) : (
              <>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Name</TableCell>
                        <TableCell>Category</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell align="right">Price</TableCell>
                        <TableCell>Stock</TableCell>
                        <TableCell>Created At</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {products.map((product) => (
                        <TableRow key={product.productId} hover>
                          <TableCell>
                            <Stack direction="row" spacing={1.5} alignItems="center">
                              <Box className="product-thumb search-thumb">
                                <Typography className="product-thumb-fallback">
                                  {getResultInitial(product.name)}
                                </Typography>
                              </Box>
                              <Box sx={{ minWidth: 0 }}>
                                <Typography className="table-primary" noWrap>
                                  {product.name}
                                </Typography>
                                <Typography className="table-secondary" noWrap>
                                  {product.description || "No description"}
                                </Typography>
                                <Typography className="entity-id" noWrap>
                                  {product.productId}
                                </Typography>
                              </Box>
                            </Stack>
                          </TableCell>
                          <TableCell>
                            <Chip
                              size="small"
                              label={product.categoryName || "Uncategorized"}
                              className="soft-chip"
                              variant="outlined"
                            />
                          </TableCell>
                          <TableCell>
                            <Chip
                              size="small"
                              label={product.status || "UNKNOWN"}
                              color={
                                product.status === "ACTIVE"
                                  ? "success"
                                  : "default"
                              }
                              variant="outlined"
                            />
                          </TableCell>
                          <TableCell align="right">
                            <Typography className="money-value">
                              {formatPrice(product.price)}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Chip
                              size="small"
                              {...stockChipProps(product.inStock)}
                            />
                          </TableCell>
                          <TableCell>
                            {formatDateTime(product.createdAt)}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
                <Stack
                  direction={{ xs: "column", sm: "row" }}
                  alignItems="center"
                  justifyContent="space-between"
                  spacing={2}
                  sx={{
                    px: 2,
                    py: 2,
                    borderTop: "1px solid",
                    borderColor: "divider",
                  }}
                >
                  <Typography variant="body2" color="text.secondary">
                    {pageInfo.totalElements} indexed products
                  </Typography>
                  <Pagination
                    page={pageInfo.currentPage}
                    count={Math.max(pageInfo.totalPages, 1)}
                    color="primary"
                    onChange={(_, page) => loadSearch(page)}
                  />
                </Stack>
              </>
            )}
        </Paper>
      </Stack>
    </MainLayout>
  );
}
