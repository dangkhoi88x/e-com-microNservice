import {
  Alert,
  Box,
  Button,
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
import { useNavigate } from "react-router-dom";
import { PageHeader } from "../components/admin";
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
      <PageHeader
        eyebrow="Product setup"
        title="Create Product"
        description="Add a sellable item and publish it to search."
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
                helperText={
                  Number(form.quantity) === 0 ? "This product starts out of stock." : " "
                }
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
                  {submitting ? "Saving..." : "Create product"}
                </Button>
              </Stack>
            </Box>
          </Stack>
        </Box>
      </Paper>
    </MainLayout>
  );
}
