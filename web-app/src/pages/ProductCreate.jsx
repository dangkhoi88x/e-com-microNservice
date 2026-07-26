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
  TextField,
  Typography,
} from "@mui/material";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import CloudUploadOutlinedIcon from "@mui/icons-material/CloudUploadOutlined";
import ArrowUpwardOutlinedIcon from "@mui/icons-material/ArrowUpwardOutlined";
import ArrowDownwardOutlinedIcon from "@mui/icons-material/ArrowDownwardOutlined";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getCategories } from "../services/categoryService";
import { createSellerProduct } from "../services/productService";
import { deleteMedia, uploadProductImage } from "../services/mediaService";
import { ProductOptionsEditor, VariantAttributeFields } from "../components/products/ProductOptionsEditor";
import { resolveOptionImageUrls, toOptionsPayload } from "../components/products/productOptionUtils";

const initialForm = {
  categoryId: "",
  name: "",
  description: "",
  price: "",
  quantity: "",
  status: "ACTIVE",
  imageFiles: [],
  options: [],
  variants: [],
};

const emptyVariant = {
  sku: "",
  attributes: {},
  price: "",
  quantity: "",
  imageUrl: "",
  imageFile: null,
  imagePreviewUrl: "",
  status: "ACTIVE",
};

const MAX_PRODUCT_IMAGES = 8;
const MAX_IMAGE_BYTES = 6 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

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

  const selectVariantImage = (index) => (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (!ALLOWED_IMAGE_TYPES.has(file.type) || file.size > MAX_IMAGE_BYTES) {
      setErrorMessage("Ảnh variant phải là JPEG, PNG hoặc WebP và không quá 6 MB.");
      return;
    }
    setForm((current) => ({
      ...current,
      variants: current.variants.map((variant, itemIndex) => {
        if (itemIndex !== index) return variant;
        if (variant.imagePreviewUrl) URL.revokeObjectURL(variant.imagePreviewUrl);
        return { ...variant, imageFile: file, imagePreviewUrl: URL.createObjectURL(file), imageUrl: "" };
      }),
    }));
  };

  const handleImageSelection = (event) => {
    const selected = Array.from(event.target.files || []);
    const validFiles = selected.filter((file) => ALLOWED_IMAGE_TYPES.has(file.type) && file.size <= MAX_IMAGE_BYTES);
    if (validFiles.length !== selected.length) {
      setErrorMessage("Chỉ chọn JPEG, PNG hoặc WebP, mỗi ảnh tối đa 6 MB.");
    }
    setForm((current) => {
      const remaining = MAX_PRODUCT_IMAGES - current.imageFiles.length;
      if (validFiles.length > remaining) setErrorMessage(`Tối đa ${MAX_PRODUCT_IMAGES} ảnh cho một sản phẩm.`);
      return {
        ...current,
        imageFiles: [...current.imageFiles, ...validFiles.slice(0, Math.max(0, remaining)).map((file) => ({
          id: `${file.name}-${file.lastModified}-${crypto.randomUUID()}`,
          file,
          previewUrl: URL.createObjectURL(file),
        }))],
      };
    });
    event.target.value = "";
  };

  const removeImage = (id) => setForm((current) => {
    const item = current.imageFiles.find((image) => image.id === id);
    if (item) URL.revokeObjectURL(item.previewUrl);
    return { ...current, imageFiles: current.imageFiles.filter((image) => image.id !== id) };
  });

  const moveImage = (index, direction) => setForm((current) => {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= current.imageFiles.length) return current;
    const imageFiles = [...current.imageFiles];
    [imageFiles[index], imageFiles[targetIndex]] = [imageFiles[targetIndex], imageFiles[index]];
    return { ...current, imageFiles };
  });

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");

    const uploadedMedia = [];

    try {
      const images = [];
      for (const image of form.imageFiles) {
        const media = await uploadProductImage(image.file);
        uploadedMedia.push(media);
        images.push(media);
      }
      const variants = [];
      for (const variant of form.variants) {
        let imageUrl = variant.imageUrl;
        if (variant.imageFile) {
          const media = await uploadProductImage(variant.imageFile);
          uploadedMedia.push(media);
          imageUrl = media.contentUrl;
        }
        variants.push(toVariantPayload({ ...variant, imageUrl }));
      }
      const options = await resolveOptionImageUrls(form.options, uploadProductImage, (media) => uploadedMedia.push(media));
      const payload = {
        categoryId: form.categoryId,
        name: form.name.trim(),
        description: form.description.trim(),
        price: Number(form.price),
        quantity: Number(form.quantity),
        images: images.map((media, index) => ({
          url: media.contentUrl,
          isPrimary: index === 0,
          displayOrder: index + 1,
        })),
        options: toOptionsPayload(options),
        variants,
      };
      await createSellerProduct(payload);
      navigate("/seller/products");
    } catch (error) {
      uploadedMedia.forEach((media) => deleteMedia(media.id).catch(() => {}));
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
            <ProductOptionsEditor options={form.options} onChange={(options) => setForm((current) => ({ ...current, options }))} onError={setErrorMessage} />

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
              <Button component="label" variant="outlined" startIcon={<CloudUploadOutlinedIcon />} disabled={form.imageFiles.length >= MAX_PRODUCT_IMAGES}>
                Thêm ảnh sản phẩm
                <input hidden type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={handleImageSelection} />
              </Button>
              <Typography variant="caption" color="text.secondary">JPEG, PNG hoặc WebP · tối đa 6 MB/ảnh · tối đa {MAX_PRODUCT_IMAGES} ảnh. Ảnh đầu tiên là ảnh chính.</Typography>
              <Stack spacing={1}>
                {form.imageFiles.map((image, index) => (
                  <Stack key={image.id} direction="row" spacing={1} alignItems="center" sx={{ p: 1, border: "1px solid", borderColor: "divider", borderRadius: 1 }}>
                    <Box component="img" src={image.previewUrl} alt="Xem trước ảnh sản phẩm" sx={{ width: 56, height: 56, objectFit: "cover", borderRadius: 1 }} />
                    <Box sx={{ flex: 1, minWidth: 0 }}><Typography noWrap fontWeight={index === 0 ? 800 : 500}>{index + 1}. {image.file.name}{index === 0 ? " · Ảnh chính" : ""}</Typography><Typography variant="caption" color="text.secondary">{Math.ceil(image.file.size / 1024)} KB</Typography></Box>
                    <IconButton size="small" disabled={index === 0} onClick={() => moveImage(index, -1)} aria-label="Di chuyển ảnh lên"><ArrowUpwardOutlinedIcon fontSize="small" /></IconButton>
                    <IconButton size="small" disabled={index === form.imageFiles.length - 1} onClick={() => moveImage(index, 1)} aria-label="Di chuyển ảnh xuống"><ArrowDownwardOutlinedIcon fontSize="small" /></IconButton>
                    <IconButton size="small" color="error" onClick={() => removeImage(image.id)} aria-label="Xóa ảnh"><DeleteOutlineOutlinedIcon fontSize="small" /></IconButton>
                  </Stack>
                ))}
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
                      <Stack direction="row" spacing={1} alignItems="center">
                        {variant.imagePreviewUrl && <Box component="img" src={variant.imagePreviewUrl} alt="Ảnh variant" sx={{ width: 42, height: 42, borderRadius: 1, objectFit: "cover" }} />}
                        <Button component="label" size="small" variant="outlined" startIcon={<CloudUploadOutlinedIcon />}>
                          {variant.imageFile ? "Đổi ảnh" : "Ảnh variant"}
                          <input hidden type="file" accept="image/jpeg,image/png,image/webp" onChange={selectVariantImage(index)} />
                        </Button>
                      </Stack>
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
