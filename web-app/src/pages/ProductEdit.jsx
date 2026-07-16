import {
  Alert,
  Box,
  Button,
  CircularProgress,
  FormControl,
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
import { useNavigate, useParams } from "react-router-dom";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getCategories } from "../services/categoryService";
import { getProductById, updateProduct } from "../services/productService";

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

const toForm = (product) => {
  const primaryImage =
    product?.images?.find((image) => image.isPrimary) || product?.images?.[0];

  return {
    categoryId: product?.categoryId || "",
    name: product?.name || "",
    description: product?.description || "",
    price: product?.price ?? "",
    quantity: product?.quantity ?? "",
    status: product?.status || "ACTIVE",
    imageUrl: primaryImage?.url || "",
    isPrimary: primaryImage?.isPrimary ?? true,
    displayOrder: primaryImage?.displayOrder || 1,
  };
};

export default function ProductEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      setErrorMessage("");

      try {
        const [categoryData, productData] = await Promise.all([
          getCategories(),
          getProductById(id),
        ]);
        setCategories(categoryData);
        setForm(toForm(productData));
      } catch (error) {
        setErrorMessage(
          error.response?.data?.message || "Could not load product details.",
        );
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [id]);

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
      await updateProduct(id, payload);
      navigate("/products");
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not update product.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Product setup"
        title="Edit Product"
        description="Update product information and publish the changes."
        actions={
        <Button
          variant="outlined"
          startIcon={<ArrowBackOutlinedIcon />}
          onClick={() => navigate("/products")}
        >
          Back
        </Button>
        }
      />

      <Paper
        elevation={0}
        className="product-form-card"
        sx={{ mt: 3, p: { xs: 2.5, md: 3 }, border: "1px solid", borderColor: "divider" }}
      >
        {errorMessage && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errorMessage}
          </Alert>
        )}

        {loading ? (
          <Stack alignItems="center" justifyContent="center" sx={{ minHeight: 280 }}>
            <CircularProgress />
          </Stack>
        ) : (
          <Box component="form" onSubmit={handleSubmit}>
            <Stack spacing={3}>
              <Box>
                <Typography className="form-section-kicker">Basic information</Typography>
                <Stack spacing={2}>
                  <TextField
                    label="Product name"
                    value={form.name}
                    onChange={setField("name")}
                    fullWidth
                    required
                  />
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
                  <TextField
                    label="Description"
                    value={form.description}
                    onChange={setField("description")}
                    fullWidth
                    multiline
                    minRows={3}
                  />
                </Stack>
              </Box>

              <Box>
                <Typography className="form-section-kicker">Pricing & inventory</Typography>
                <Stack spacing={2}>
                  <TextField
                    label="Price"
                    type="number"
                    value={form.price}
                    onChange={setField("price")}
                    fullWidth
                    required
                    inputProps={{ min: 1 }}
                  />
                  <TextField
                    label="Quantity"
                    type="number"
                    value={form.quantity}
                    onChange={setField("quantity")}
                    fullWidth
                    required
                    inputProps={{ min: 0 }}
                    helperText="Changing this updates product quantity data."
                  />
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
                </Stack>
              </Box>

              <Box>
                <Typography className="form-section-kicker">Product image</Typography>
                <Stack spacing={2}>
                  <TextField
                    label="Image URL"
                    value={form.imageUrl}
                    onChange={setField("imageUrl")}
                    fullWidth
                  />
                  <TextField
                    label="Order"
                    type="number"
                    value={form.displayOrder}
                    onChange={setField("displayOrder")}
                    fullWidth
                    inputProps={{ min: 1 }}
                  />
                  <Stack
                    direction="row"
                    alignItems="center"
                    spacing={1}
                    className="primary-switch-row"
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
                </Stack>
              </Box>

              <Box className="form-action-bar">
                <Stack direction={{ xs: "column-reverse", sm: "row" }} justifyContent="flex-end" spacing={1}>
                  <Button onClick={() => navigate("/products")}>Cancel</Button>
                  <Button
                    type="submit"
                    variant="contained"
                    startIcon={<SaveOutlinedIcon />}
                    disabled={submitting}
                  >
                    {submitting ? "Saving..." : "Update product"}
                  </Button>
                </Stack>
              </Box>
            </Stack>
          </Box>
        )}
      </Paper>
    </MainLayout>
  );
}
