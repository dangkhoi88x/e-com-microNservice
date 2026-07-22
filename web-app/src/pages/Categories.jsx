import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import { useEffect, useState } from "react";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import {
  createCategory,
  deleteCategory,
  getCategories,
  updateCategory,
} from "../services/categoryService";
import { formatDateTime } from "../utils/dateTimeUtils";

const emptyForm = {
  id: "",
  name: "",
  description: "",
};

export default function Categories() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [errorMessage, setErrorMessage] = useState("");

  const editing = Boolean(form.id);

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

  const openCreateDialog = () => {
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openEditDialog = (category) => {
    setForm({
      id: category.id,
      name: category.name || "",
      description: category.description || "",
    });
    setDialogOpen(true);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");

    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
    };

    try {
      if (editing) {
        await updateCategory(form.id, payload);
      } else {
        await createCategory(payload);
      }
      setDialogOpen(false);
      await loadCategories();
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not save category.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (category) => {
    if (!window.confirm(`Delete category "${category.name}"?`)) return;

    try {
      await deleteCategory(category.id);
      await loadCategories();
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not delete category.",
      );
    }
  };

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Store service"
        title="Categories"
        description="Organize product categories and slugs."
        actions={
          <>
          <Button
            variant="outlined"
            startIcon={<RefreshOutlinedIcon />}
            onClick={loadCategories}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddOutlinedIcon />}
            onClick={openCreateDialog}
          >
            New category
          </Button>
          </>
        }
      />

      <Paper
        className="admin-data-panel"
        elevation={0}
        sx={{
          mt: 3,
          border: "1px solid",
          borderColor: "divider",
          overflow: "hidden",
        }}
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
        ) : categories.length === 0 ? (
          <Box sx={{ p: 4 }}>
            <Typography fontWeight={800}>No categories yet</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Create a category and it will appear here.
            </Typography>
          </Box>
        ) : (
          <>
            <Stack
              direction={{ xs: "column", sm: "row" }}
              justifyContent="space-between"
              alignItems={{ xs: "stretch", sm: "center" }}
              spacing={2}
              className="panel-summary"
            >
              <Stack direction="row" spacing={1.25} alignItems="center" className="section-library-heading">
                <Box className="section-heading-icon section-heading-icon-blue"><CategoryOutlinedIcon /></Box>
                <Box>
                <Typography className="section-heading-title">Catalog taxonomy</Typography>
                <Typography className="section-heading-description">
                  {categories.length} active category records
                </Typography>
                </Box>
              </Stack>
              <Chip
                label={`${categories.length} categories`}
                color="primary"
                variant="outlined"
              />
            </Stack>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Slug</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Created At</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {categories.map((category) => (
                    <TableRow key={category.id} hover>
                      <TableCell>
                        <Stack direction="row" spacing={1.5} alignItems="center">
                          <Box className="category-mark">
                            <CategoryOutlinedIcon fontSize="small" />
                          </Box>
                          <Box sx={{ minWidth: 0 }}>
                            <Typography className="table-primary" noWrap>
                              {category.name}
                            </Typography>
                            <Typography className="entity-id" noWrap>
                              {category.id}
                            </Typography>
                          </Box>
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={category.slug || "no-slug"}
                          className="soft-chip"
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        <Typography className="table-secondary" noWrap>
                          {category.description || "No description"}
                        </Typography>
                      </TableCell>
                      <TableCell>{formatDateTime(category.createdAt)}</TableCell>
                      <TableCell align="right">
                        <Box className="row-actions">
                          <Tooltip title="Edit">
                            <IconButton onClick={() => openEditDialog(category)}>
                              <EditOutlinedIcon />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton
                              color="error"
                              onClick={() => handleDelete(category)}
                            >
                              <DeleteOutlineOutlinedIcon />
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </>
        )}
      </Paper>

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        fullWidth
        maxWidth="sm"
      >
        <Box component="form" onSubmit={handleSubmit}>
          <DialogTitle>
            {editing ? "Update category" : "Create category"}
          </DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ pt: 1 }}>
              <TextField
                label="Name"
                value={form.name}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
                fullWidth
                required
              />
              <TextField
                label="Description"
                value={form.description}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    description: event.target.value,
                  }))
                }
                fullWidth
                multiline
                minRows={3}
              />
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={submitting}>
              {submitting ? "Saving..." : "Save"}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>
    </MainLayout>
  );
}
