import {
  Alert,
  Box,
  Button,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { getCategories } from "../services/categoryService";
import { createProduct } from "../services/productService";

const initialForm = {
  categoryId: "",
  name: "",
  description: "",
  price: "",
  quantity: "",
  status: "ACTIVE",
  imageUrl: "",
  isPrimary: true,
  displayOrder: 1,
};

export default function ProductCreate() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    const loadCategories = async () => {
      try {
        setCategories(await getCategories());
      } catch {
        setCategories([]);
      }
    };

    loadCategories();
  }, []);

  const setField = (field) => (event) => {
    setForm((current) => ({
      ...current,
      [field]: event.target.value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");

    const images = form.imageUrl.trim()
      ? [
          {
            url: form.imageUrl.trim(),
            isPrimary: form.isPrimary,
            displayOrder: Number(form.displayOrder) || 1,
          },
        ]
      : [];

    const payload = {
      categoryId: form.categoryId,
      name: form.name.trim(),
      description: form.description.trim(),
      price: Number(form.price),
      quantity: Number(form.quantity),
      images,
      status: form.status,
    };

    try {
      await createProduct(payload);
      navigate("/products");
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not create product.",
      );
    } finally {
      setSubmitting(false);
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
            Create Product
          </Typography>
          <Typography color="text.secondary">
            Add a sellable item and publish it to search.
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<ArrowBackOutlinedIcon />}
          onClick={() => navigate("/products")}
        >
          Back
        </Button>
      </Stack>

      <Paper
        elevation={0}
        sx={{ mt: 3, p: 3, border: "1px solid", borderColor: "divider" }}
      >
        {errorMessage && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errorMessage}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit}>
          <Grid container spacing={2.5}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Product name"
                value={form.name}
                onChange={setField("name")}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth required>
                <InputLabel>Category</InputLabel>
                <Select
                  label="Category"
                  value={form.categoryId}
                  onChange={setField("categoryId")}
                >
                  {categories.map((category) => (
                    <MenuItem key={category.id} value={category.id}>
                      {category.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Description"
                value={form.description}
                onChange={setField("description")}
                fullWidth
                multiline
                minRows={3}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Price"
                type="number"
                value={form.price}
                onChange={setField("price")}
                fullWidth
                required
                inputProps={{ min: 1 }}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Quantity"
                type="number"
                value={form.quantity}
                onChange={setField("quantity")}
                fullWidth
                required
                inputProps={{ min: 0 }}
                helperText={
                  Number(form.quantity) === 0 ? "This product starts out of stock." : " "
                }
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <FormControl fullWidth required>
                <InputLabel>Status</InputLabel>
                <Select
                  label="Status"
                  value={form.status}
                  onChange={setField("status")}
                >
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="INACTIVE">Inactive</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={8}>
              <TextField
                label="Image URL"
                value={form.imageUrl}
                onChange={setField("imageUrl")}
                fullWidth
              />
            </Grid>
            <Grid item xs={6} md={2}>
              <TextField
                label="Order"
                type="number"
                value={form.displayOrder}
                onChange={setField("displayOrder")}
                fullWidth
                inputProps={{ min: 1 }}
              />
            </Grid>
            <Grid item xs={6} md={2}>
              <Stack
                direction="row"
                alignItems="center"
                spacing={1}
                sx={{ height: "100%" }}
              >
                <Switch
                  checked={form.isPrimary}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      isPrimary: event.target.checked,
                    }))
                  }
                />
                <Typography fontWeight={700}>Primary</Typography>
              </Stack>
            </Grid>
            <Grid item xs={12}>
              <Stack direction="row" justifyContent="flex-end" spacing={1}>
                <Button onClick={() => navigate("/products")}>Cancel</Button>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<SaveOutlinedIcon />}
                  disabled={submitting}
                >
                  {submitting ? "Saving..." : "Create product"}
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </Box>
      </Paper>
    </MainLayout>
  );
}
