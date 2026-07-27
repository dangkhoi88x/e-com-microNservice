import {
  Alert,
  Box,
  Button,
  CircularProgress,
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
import { useNavigate, useParams } from "react-router-dom";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getCategories } from "../services/categoryService";
import { getSellerProductById, updateSellerProduct } from "../services/productService";
import { deleteMedia, uploadProductImage } from "../services/mediaService";
import { ProductOptionsEditor, VariantAttributeFields } from "../components/products/ProductOptionsEditor";
import { resolveOptionImageUrls, toOptionsPayload } from "../components/products/productOptionUtils";

const initialForm = {
  categoryId: "",
  name: "",
  description: "",
  price: "",
  quantity: "",
  imageFiles: [],
  options: [],
  variants: [],
};

const emptyVariant = {
  id: "",
  sku: "",
  attributes: {},
  price: "",
  quantity: "",
  imageUrl: "",
  originalImageUrl: "",
  imageFile: null,
  imagePreviewUrl: "",
  status: "ACTIVE",
};

const MAX_PRODUCT_IMAGES = 8;
const MAX_IMAGE_BYTES = 6 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

const toVariantForm = (variant) => ({
  id: variant?.id || "",
  sku: variant?.sku || "",
  attributes: variant?.attributes || {},
  price: variant?.price ?? "",
  quantity: variant?.quantity ?? "",
  imageUrl: variant?.imageUrl || "",
  originalImageUrl: variant?.imageUrl || "",
  imageFile: null,
  imagePreviewUrl: "",
  status: variant?.status || "ACTIVE",
});

const toVariantPayload = (variant) => ({
  id: variant.id || null,
  sku: variant.sku.trim() || null,
  attributes: Object.fromEntries(Object.entries(variant.attributes || {}).filter(([, value]) => value).map(([key, value]) => [key.trim().toLowerCase(), value])),
  price: variant.price === "" ? null : Number(variant.price),
  quantity: variant.quantity === "" ? null : Number(variant.quantity),
  imageUrl: variant.imageUrl.trim() || null,
  status: variant.status,
});

const toForm = (product) => {
  const imageFiles = [...(product?.images || [])]
    .sort((left, right) => (left.displayOrder ?? Number.MAX_SAFE_INTEGER) - (right.displayOrder ?? Number.MAX_SAFE_INTEGER))
    .map((image, index) => ({ id: `existing-${index}-${image.url}`, url: image.url, existing: true }));
  return {
    categoryId: product?.categoryId || "",
    name: product?.name || "",
    description: product?.description || "",
    price: product?.price ?? "",
    quantity: product?.quantity ?? "",
    imageFiles,
    options: (product?.options || []).map((option) => ({ ...option, values: option.values || [] })),
    variants: (product?.variants || []).map(toVariantForm),
  };
};

const getMediaIdFromUrl = (url) => {
  const match = url?.match(/\/api\/v1\/media\/([0-9a-f-]{36})\/content(?:$|[?#])/i);
  return match?.[1] || null;
};

const getOriginalMediaUrls = (product) => [
  ...(product?.images || []).map((image) => image.url),
  ...(product?.options || []).flatMap((option) => (option.values || []).map((value) => value.imageUrl)),
  ...(product?.variants || []).map((variant) => variant.imageUrl),
].filter(Boolean);

export default function ProductEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [originalMediaUrls, setOriginalMediaUrls] = useState([]);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      setErrorMessage("");

      try {
        const [categoryData, productData] = await Promise.all([
          getCategories(),
          getSellerProductById(id),
        ]);
        setCategories(categoryData);
        setForm(toForm(productData));
        setOriginalMediaUrls(getOriginalMediaUrls(productData));
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

  const handleImageSelection = (event) => {
    const selected = Array.from(event.target.files || []);
    const validFiles = selected.filter((file) => ALLOWED_IMAGE_TYPES.has(file.type) && file.size <= MAX_IMAGE_BYTES);
    if (validFiles.length !== selected.length) setErrorMessage("Chỉ chọn JPEG, PNG hoặc WebP, mỗi ảnh tối đa 6 MB.");
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

  const removeImage = (imageId) => setForm((current) => {
    const image = current.imageFiles.find((item) => item.id === imageId);
    if (image?.previewUrl) URL.revokeObjectURL(image.previewUrl);
    return { ...current, imageFiles: current.imageFiles.filter((item) => item.id !== imageId) };
  });

  const moveImage = (index, direction) => setForm((current) => {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= current.imageFiles.length) return current;
    const imageFiles = [...current.imageFiles];
    [imageFiles[index], imageFiles[targetIndex]] = [imageFiles[targetIndex], imageFiles[index]];
    return { ...current, imageFiles };
  });

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
        return { ...variant, imageFile: file, imagePreviewUrl: URL.createObjectURL(file) };
      }),
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");

    const uploadedMedia = [];

    try {
      const images = [];
      for (const image of form.imageFiles) {
        let url = image.url;
        if (image.file) {
          const media = await uploadProductImage(image.file);
          uploadedMedia.push(media);
          url = media.contentUrl;
        }
        images.push({ url, isPrimary: images.length === 0, displayOrder: images.length + 1 });
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
        images,
        options: toOptionsPayload(options),
        variants,
      };
      await updateSellerProduct(id, payload);
      const retainedUrls = new Set([
        ...images.map((image) => image.url),
        ...options.flatMap((option) => option.values.map((value) => value.imageUrl)),
        ...variants.map((variant) => variant.imageUrl),
      ].filter(Boolean));
      await Promise.allSettled(
        originalMediaUrls
          .filter((url) => !retainedUrls.has(url))
          .map(getMediaIdFromUrl)
          .filter(Boolean)
          .map((mediaId) => deleteMedia(mediaId)),
      );
      navigate("/seller/products");
    } catch (error) {
      uploadedMedia.forEach((media) => deleteMedia(media.id).catch(() => {}));
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

        {loading ? (
          <Stack alignItems="center" justifyContent="center" sx={{ minHeight: 280 }}>
            <CircularProgress />
          </Stack>
        ) : (
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
                    helperText="Changing this updates product quantity data."
                  />
                </Stack>
              </Box>

              <Box>
                <Typography className="form-section-kicker">Product images</Typography>
                <Stack spacing={2}>
                  <Button component="label" variant="outlined" startIcon={<CloudUploadOutlinedIcon />} disabled={form.imageFiles.length >= MAX_PRODUCT_IMAGES}>
                    Thêm ảnh sản phẩm
                    <input hidden type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={handleImageSelection} />
                  </Button>
                  <Typography variant="caption" color="text.secondary">JPEG, PNG hoặc WebP · tối đa 6 MB/ảnh · tối đa {MAX_PRODUCT_IMAGES} ảnh. Ảnh đầu tiên là ảnh chính.</Typography>
                  <Stack spacing={1}>
                    {form.imageFiles.map((image, index) => (
                      <Stack key={image.id} direction="row" spacing={1} alignItems="center" sx={{ p: 1, border: "1px solid", borderColor: "divider", borderRadius: 1 }}>
                        <Box component="img" src={image.previewUrl || image.url} alt="Xem trước ảnh sản phẩm" sx={{ width: 56, height: 56, objectFit: "cover", borderRadius: 1 }} />
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Typography noWrap fontWeight={index === 0 ? 800 : 500}>{index + 1}. {image.file?.name || "Ảnh đã lưu"}{index === 0 ? " · Ảnh chính" : ""}</Typography>
                          <Typography variant="caption" color="text.secondary">{image.file ? `${Math.ceil(image.file.size / 1024)} KB · mới` : "Đã lưu trên S3"}</Typography>
                        </Box>
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
                      <Box className="variant-editor-row" key={variant.id || `variant-${index}`}>
                        <TextField label="SKU" value={variant.sku} onChange={setVariantField(index, "sku")} />
                        <VariantAttributeFields options={form.options} attributes={variant.attributes} onChange={(name, value) => setVariantAttribute(index, name, value)} />
                        <TextField label="Price" type="number" value={variant.price} onChange={setVariantField(index, "price")} />
                        <TextField label="Stock" type="number" value={variant.quantity} onChange={setVariantField(index, "quantity")} />
                        <Stack direction="row" spacing={1} alignItems="center">
                          {(variant.imagePreviewUrl || variant.imageUrl) && <Box component="img" src={variant.imagePreviewUrl || variant.imageUrl} alt="Ảnh variant" sx={{ width: 42, height: 42, borderRadius: 1, objectFit: "cover" }} />}
                          <Button component="label" size="small" variant="outlined" startIcon={<CloudUploadOutlinedIcon />}>
                            {variant.imageFile ? "Đổi ảnh" : variant.imageUrl ? "Thay ảnh" : "Ảnh variant"}
                            <input hidden type="file" accept="image/jpeg,image/png,image/webp" onChange={selectVariantImage(index)} />
                          </Button>
                        </Stack>
                        <FormControl>
                          <InputLabel>Status</InputLabel>
                          <Select label="Status" value={variant.status} onChange={setVariantField(index, "status")}>
                            <MenuItem value="ACTIVE">Active</MenuItem>
                            <MenuItem value="INACTIVE">Inactive</MenuItem>
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
