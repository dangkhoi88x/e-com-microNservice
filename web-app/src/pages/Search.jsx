import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  FormControl,
  Grid,
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
import { useEffect, useState } from "react";
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

  return (
    <MainLayout>
      <Box>
        <Typography variant="h4" fontWeight={900}>
          Search
        </Typography>
        <Typography color="text.secondary">
          Query Elasticsearch with filters, pagination, and aggregations.
        </Typography>
      </Box>

      <Grid container spacing={3} sx={{ mt: 0.5 }}>
        <Grid item xs={12} lg={3}>
          <Paper
            elevation={0}
            sx={{ p: 2, border: "1px solid", borderColor: "divider" }}
          >
            <Typography fontWeight={900}>Filters</Typography>
            <Stack spacing={2} sx={{ mt: 2 }}>
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
              <FormControl fullWidth>
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
              <FormControl fullWidth>
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
              <Stack direction="row" spacing={1}>
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
              </Stack>
              <Stack direction="row" spacing={1}>
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
            </Stack>

            <Divider sx={{ my: 2 }} />

            <Typography fontWeight={900}>Aggregations</Typography>
            {loading ? (
              <LinearProgress sx={{ mt: 2 }} />
            ) : (
              <Stack spacing={2} sx={{ mt: 2 }}>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Price stats
                  </Typography>
                  <Typography fontWeight={800}>
                    {formatPrice(priceStats.min)} - {formatPrice(priceStats.max)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Avg {formatPrice(priceStats.avg)} · {priceStats.count || 0} items
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Categories
                  </Typography>
                  <Stack spacing={1}>
                    {(aggregations.categories || []).map((category) => (
                      <Stack
                        key={category.name}
                        direction="row"
                        justifyContent="space-between"
                        alignItems="center"
                      >
                        <Typography variant="body2">{category.name}</Typography>
                        <Chip size="small" label={category.count} />
                      </Stack>
                    ))}
                  </Stack>
                </Box>

                <Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Price ranges
                  </Typography>
                  <Stack spacing={1}>
                    {(aggregations.priceRanges || []).map((range) => (
                      <Stack
                        key={range.range}
                        direction="row"
                        justifyContent="space-between"
                      >
                        <Typography variant="body2">{range.range}</Typography>
                        <Chip size="small" label={range.count} />
                      </Stack>
                    ))}
                  </Stack>
                </Box>
              </Stack>
            )}
          </Paper>
        </Grid>

        <Grid item xs={12} lg={9}>
          <Paper
            elevation={0}
            sx={{ border: "1px solid", borderColor: "divider", overflow: "hidden" }}
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
                            <Typography fontWeight={800}>
                              {product.name}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              {product.description || "No description"}
                            </Typography>
                          </TableCell>
                          <TableCell>{product.categoryName || "-"}</TableCell>
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
                            {formatPrice(product.price)}
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
        </Grid>
      </Grid>
    </MainLayout>
  );
}
