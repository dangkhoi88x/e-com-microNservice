import {
  Alert,
  Box,
  Button,
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
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import { useEffect, useState } from "react";
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
      <Stack
        direction={{ xs: "column", sm: "row" }}
        justifyContent="space-between"
        alignItems={{ xs: "stretch", sm: "center" }}
        spacing={2}
      >
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
          <Button
            variant="contained"
            startIcon={<AddOutlinedIcon />}
            onClick={openCreateDialog}
          >
            New category
          </Button>
        </Stack>
      </Stack>

      <Paper
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
                      <Typography fontWeight={800}>{category.name}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {category.id}
                      </Typography>
                    </TableCell>
                    <TableCell>{category.slug || "-"}</TableCell>
                    <TableCell>{category.description || "-"}</TableCell>
                    <TableCell>{formatDateTime(category.createdAt)}</TableCell>
                    <TableCell align="right">
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
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
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
