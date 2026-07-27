import { useState } from "react";
import { Alert, Box, Button, Paper, Stack, TextField } from "@mui/material";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { createShipper } from "../services/shipperService";

const emptyForm = { email: "", password: "", firstName: "", lastName: "" };

export default function AdminShippers() {
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const shipper = await createShipper(form);
      setForm(emptyForm);
      setSuccess(`Shipper account created for ${shipper?.email || "the new user"}.`);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Could not create shipper account.");
    } finally {
      setSaving(false);
    }
  };

  return <MainLayout>
    <PageHeader eyebrow="Delivery operations" title="Create shipper" description="Create a delivery account with ROLE_SHIPPER." />
    <Paper component="form" onSubmit={submit} className="admin-data-panel" elevation={0} sx={{ maxWidth: 680, mt: 3, p: 3 }}>
      <Stack spacing={2}>
        {error && <Alert severity="error" onClose={() => setError("")}>{error}</Alert>}
        {success && <Alert severity="success" onClose={() => setSuccess("")}>{success}</Alert>}
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField label="First name" value={form.firstName} onChange={update("firstName")} required fullWidth />
          <TextField label="Last name" value={form.lastName} onChange={update("lastName")} required fullWidth />
        </Stack>
        <TextField label="Email" type="email" value={form.email} onChange={update("email")} required fullWidth />
        <TextField label="Temporary password" type="password" value={form.password} onChange={update("password")} inputProps={{ minLength: 8 }} required fullWidth />
        <Box><Button type="submit" variant="contained" startIcon={<LocalShippingOutlinedIcon />} disabled={saving}>{saving ? "Creating..." : "Create shipper"}</Button></Box>
      </Stack>
    </Paper>
  </MainLayout>;
}
