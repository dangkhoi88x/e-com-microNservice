import {
  Alert,
  Box,
  Button,
  FormControl,
  IconButton,
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
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getCategories } from "../services/categoryService";
import { createSellerProduct } from "../services/productService";
import { ProductOptionsEditor, VariantAttributeFields } from "../components/products/ProductOptionsEditor";
import { toOptionsPayload } from "../components/products/productOptionUtils";

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
  options: [],
  variants: [],
};

const emptyVariant = {
  sku: "",
  attributes: {},
  price: "",
  quantity: "",
  imageUrl: "",
  status: "ACTIVE",
};

const toVariantPayload = (variant) => ({
  sku: variant.sku.trim() || null,
  attributes: Object.fromEntries(Object.entries(variant.attributes || {}).filter(([, value]) => value).map(([key, value]) => [key.trim().toLowerCase(), value])),
  price: variant.price === "" ? null : Number(variant.price),
  quantity: variant.quantity === "" ? null : Number(variant.quantity),
  imageUrl: variant.imageUrl.trim() || null,
  status: variant.status,
});

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

  const addVariant = () => {
    setForm((current) => ({
      ...current,
      variants: [...current.variants, { ...emptyVariant, attributes: {} }],
    }));
  };

  const setVariantAttribute = (index, name, value) => setForm((current) => ({
    ...current,
    variants: current.variants.map((variant, itemIndex) => itemIndex === index ? { ...variant, attributes: { ...variant.attributes, [name]: value } } : variant),
  }));

  const removeVariant = (index) => {
    setForm((current) => ({
      ...current,
      variants: current.variants.filter((_, itemIndex) => itemIndex !== index),
    }));
  };

  const setVariantField = (index, field) => (event) => {
    setForm((current) => ({
      ...current,
      variants: current.variants.map((variant, itemIndex) =>
        itemIndex === index
          ? { ...variant, [field]: event.target.value }
          : variant,
      ),
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
      options: toOptionsPayload(form.options),
      variants: form.variants.map(toVariantPayload),
    };

    try {
      await createSellerProduct(payload);
      navigate("/seller/products");
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
        title="Create product draft"
        description="Your product will be submitted for marketplace approval after you finish the draft."
        actions={
        <Button
          variant="outlined"
          startIcon={<ArrowBackOutlinedIcon />}
          onClick={() => navigate("/seller/products")}
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
            <ProductOptionsEditor options={form.options} onChange={(options) => setForm((current) => ({ ...current, options }))} />

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

            <Box>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography className="form-section-kicker">Variants / SKU</Typography>
                <Button
                  type="button"
                  size="small"
                  variant="outlined"
                  startIcon={<AddOutlinedIcon />}
                  onClick={addVariant}
                >
                  Add variant
                </Button>
              </Stack>
              <Stack spacing={2}>
                {form.variants.length === 0 ? (
                  <Box className="variant-empty-state">
                    Add variants after defining selectable product options.
                  </Box>
                ) : (
                  form.variants.map((variant, index) => (
                    <Box className="variant-editor-row" key={`variant-${index}`}>
                      <TextField label="SKU" value={variant.sku} onChange={setVariantField(index, "sku")} />
                      <VariantAttributeFields options={form.options} attributes={variant.attributes} onChange={(name, value) => setVariantAttribute(index, name, value)} />
                      <TextField label="Price" type="number" value={variant.price} onChange={setVariantField(index, "price")} />
                      <TextField label="Stock" type="number" value={variant.quantity} onChange={setVariantField(index, "quantity")} />
                      <TextField label="Image URL" value={variant.imageUrl} onChange={setVariantField(index, "imageUrl")} />
                      <FormControl>
                        <InputLabel>Status</InputLabel>
                        <Select label="Status" value="DRAFT" disabled>
                          <MenuItem value="DRAFT">Draft</MenuItem>
                        </Select>
                      </FormControl>
                      <IconButton color="error" onClick={() => removeVariant(index)}>
                        <DeleteOutlineOutlinedIcon />
                      </IconButton>
                    </Box>
                  ))
                )}
              </Stack>
            </Box>

            <Box className="form-action-bar">
              <Stack direction={{ xs: "column-reverse", sm: "row" }} justifyContent="flex-end" spacing={1}>
                <Button onClick={() => navigate("/seller/products")}>Cancel</Button>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<SaveOutlinedIcon />}
                  disabled={submitting}
                >
                  {submitting ? "Saving..." : "Save draft"}
                </Button>
              </Stack>
            </Box>
          </Stack>
        </Box>
      </Paper>
    </MainLayout>
  );
}
