import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import { useEffect, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import { getProducts } from "../services/productService";
import { formatDateTime } from "../utils/dateTimeUtils";

const formatPrice = (value) => {
  if (value === null || value === undefined) return "-";

  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value));
};

export default function Products() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  const loadProducts = async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const data = await getProducts();
      setProducts(data);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not load products.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  return (
    <MainLayout>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Box>
          <Typography variant="h4" fontWeight={900}>
            Products
          </Typography>
          <Typography color="text.secondary">
            View and manage product catalog data.
          </Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            startIcon={<RefreshOutlinedIcon />}
            onClick={loadProducts}
          >
            Refresh
          </Button>
          <Button variant="contained" startIcon={<AddOutlinedIcon />}>
            New product
          </Button>
        </Stack>
      </Stack>

      <Paper
        elevation={0}
        sx={{ mt: 3, border: "1px solid", borderColor: "divider", overflow: "hidden" }}
      >
        {errorMessage && (
          <Alert severity="error" sx={{ m: 2 }}>
            {errorMessage}
          </Alert>
        )}

        {loading ? (
          <Stack alignItems="center" justifyContent="center" sx={{ py: 8 }}>
            <CircularProgress />
            <Typography color="text.secondary" sx={{ mt: 2 }}>
              Loading products...
            </Typography>
          </Stack>
        ) : products.length === 0 ? (
          <Box sx={{ p: 4 }}>
            <Typography fontWeight={800}>No products yet</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Create a product from the seller/admin account and it will appear here.
            </Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Price</TableCell>
                  <TableCell align="right">Quantity</TableCell>
                  <TableCell>Created At</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {products.map((product) => (
                  <TableRow key={product.id} hover>
                    <TableCell>
                      <Typography fontWeight={800}>{product.name}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {product.description || "No description"}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={product.status || "UNKNOWN"}
                        color={product.status === "ACTIVE" ? "success" : "default"}
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell align="right">{formatPrice(product.price)}</TableCell>
                    <TableCell align="right">{product.quantity ?? "-"}</TableCell>
                    <TableCell>{formatDateTime(product.createdAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
    </MainLayout>
  );
}
