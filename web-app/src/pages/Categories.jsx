import {
  Alert,
  Box,
  Button,
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
import { getCategories } from "../services/categoryService";
import { formatDateTime } from "../utils/dateTimeUtils";

export default function Categories() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  const loadCategories = async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const data = await getCategories();
      setCategories(data);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not load categories.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCategories();
  }, []);

  return (
    <MainLayout>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Box>
          <Typography variant="h4" fontWeight={900}>
            Categories
          </Typography>
          <Typography color="text.secondary">
            Organize product categories and slugs.
          </Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            startIcon={<RefreshOutlinedIcon />}
            onClick={loadCategories}
          >
            Refresh
          </Button>
          <Button variant="contained" startIcon={<AddOutlinedIcon />}>
            New category
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
              Loading categories...
            </Typography>
          </Stack>
        ) : categories.length === 0 ? (
          <Box sx={{ p: 4 }}>
            <Typography fontWeight={800}>No categories yet</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Create a category and it will appear here.
            </Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Slug</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Created At</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {categories.map((category) => (
                  <TableRow key={category.id} hover>
                    <TableCell>
                      <Typography fontWeight={800}>{category.name}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {category.id}
                      </Typography>
                    </TableCell>
                    <TableCell>{category.slug || "-"}</TableCell>
                    <TableCell>{category.description || "-"}</TableCell>
                    <TableCell>{formatDateTime(category.createdAt)}</TableCell>
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
