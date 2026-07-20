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
  Divider,
  FormControlLabel,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Switch,
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
import BoltOutlinedIcon from "@mui/icons-material/BoltOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import ToggleOffOutlinedIcon from "@mui/icons-material/ToggleOffOutlined";
import ToggleOnOutlinedIcon from "@mui/icons-material/ToggleOnOutlined";
import { useEffect, useMemo, useState } from "react";
import { PageHeader, StatCard } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import {
  createPromotion,
  deletePromotion,
  getPromotions,
  updatePromotion,
} from "../services/promotionService";

const statusOptions = ["ALL", "DRAFT", "ACTIVE", "INACTIVE", "EXPIRED"];
const statusTone = {
  ACTIVE: "success",
  DRAFT: "warning",
  INACTIVE: "default",
  EXPIRED: "error",
};

const blankForm = () => ({
  id: "",
  name: "",
  code: "",
  description: "",
  type: "PERCENTAGE",
  discountValue: "10",
  maxDiscountAmount: "",
  minOrderAmount: "0",
  startAt: toLocalInput(new Date()),
  endAt: toLocalInput(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)),
  usageLimit: "100",
  priority: "0",
  stackable: false,
  status: "DRAFT",
});

function toLocalInput(value) {
  const date = value ? new Date(value) : new Date();
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 16);
}

function formatMoney(value) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value || 0));
}

function formatDate(value) {
  return value ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "—";
}

function promotionPayload(form, includeStatus) {
  const payload = {
    name: form.name.trim(),
    description: form.description.trim() || null,
    type: form.type,
    discountValue: Number(form.discountValue),
    maxDiscountAmount: form.maxDiscountAmount === "" ? null : Number(form.maxDiscountAmount),
    minOrderAmount: Number(form.minOrderAmount || 0),
    startAt: new Date(form.startAt).toISOString(),
    endAt: new Date(form.endAt).toISOString(),
    usageLimit: Number(form.usageLimit || 0),
    applicableCategoryIds: [],
    applicableProductIds: [],
    priority: Number(form.priority || 0),
    stackable: Boolean(form.stackable),
  };
  if (!includeStatus) return { ...payload, code: form.code.trim().toUpperCase() };
  return { ...payload, status: form.status };
}

export default function Promotions() {
  const [campaigns, setCampaigns] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(blankForm);
  const [errorMessage, setErrorMessage] = useState("");

  const editing = Boolean(form.id);

  const loadCampaigns = async (status = filter) => {
    setLoading(true);
    setErrorMessage("");
    try {
      setCampaigns(await getPromotions(status));
    } catch (error) {
      const responseStatus = error.response?.status;
      setErrorMessage(
        responseStatus === 401
          ? "Your session has expired. Please log in again, then reopen Promotions."
          : responseStatus === 403
            ? "Your account is not allowed to access promotion data."
            : error.response?.data?.message || "Could not load promotion campaigns. Ensure promotion-service is running.",
      );
      setCampaigns([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadCampaigns(); }, []);

  const metrics = useMemo(() => ({
    active: campaigns.filter((campaign) => campaign.status === "ACTIVE").length,
    draft: campaigns.filter((campaign) => campaign.status === "DRAFT").length,
    redemptions: campaigns.reduce((total, campaign) => total + Number(campaign.usedCount || 0), 0),
  }), [campaigns]);

  const setField = (key, value) => setForm((current) => ({ ...current, [key]: value }));

  const openCreate = () => {
    setForm(blankForm());
    setDialogOpen(true);
  };

  const openEdit = (campaign) => {
    setForm({
      id: campaign.id,
      name: campaign.name || "",
      code: campaign.code || "",
      description: campaign.description || "",
      type: campaign.type || "PERCENTAGE",
      discountValue: String(campaign.discountValue ?? ""),
      maxDiscountAmount: campaign.maxDiscountAmount == null ? "" : String(campaign.maxDiscountAmount),
      minOrderAmount: String(campaign.minOrderAmount ?? 0),
      startAt: toLocalInput(campaign.startAt),
      endAt: toLocalInput(campaign.endAt),
      usageLimit: String(campaign.usageLimit ?? 0),
      priority: String(campaign.priority ?? 0),
      stackable: Boolean(campaign.stackable),
      status: campaign.status || "DRAFT",
    });
    setDialogOpen(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");
    try {
      if (editing) await updatePromotion(form.id, promotionPayload(form, true));
      else await createPromotion(promotionPayload(form, false));
      setDialogOpen(false);
      await loadCampaigns();
    } catch (error) {
      setErrorMessage(error.response?.data?.message || "Could not save promotion campaign.");
    } finally {
      setSubmitting(false);
    }
  };

  const changeStatus = async (campaign) => {
    const nextStatus = campaign.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      await updatePromotion(campaign.id, promotionPayload({
        ...campaign,
        startAt: toLocalInput(campaign.startAt),
        endAt: toLocalInput(campaign.endAt),
        maxDiscountAmount: campaign.maxDiscountAmount == null ? "" : String(campaign.maxDiscountAmount),
        minOrderAmount: String(campaign.minOrderAmount ?? 0),
        usageLimit: String(campaign.usageLimit ?? 0),
        priority: String(campaign.priority ?? 0),
        status: nextStatus,
      }, true));
      await loadCampaigns();
    } catch (error) {
      setErrorMessage(error.response?.data?.message || "Could not change campaign status.");
    }
  };

  const remove = async (campaign) => {
    if (!window.confirm(`Delete campaign ${campaign.code}? Campaigns that have been used will be set inactive instead.`)) return;
    try {
      await deletePromotion(campaign.id);
      await loadCampaigns();
    } catch (error) {
      setErrorMessage(error.response?.data?.message || "Could not delete promotion campaign.");
    }
  };

  const handleFilter = (value) => {
    setFilter(value);
    loadCampaigns(value);
  };

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Store service · Promotion engine"
        title="Promotions"
        description="Create discount campaigns, control availability and monitor voucher redemptions."
        actions={<Stack direction="row" spacing={1}><Button variant="outlined" startIcon={<RefreshOutlinedIcon />} onClick={() => loadCampaigns()}>Refresh</Button><Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={openCreate}>New campaign</Button></Stack>}
      />

      {errorMessage && <Alert severity="error" sx={{ mt: 2 }}>{errorMessage}</Alert>}

      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "repeat(3, 1fr)" }, gap: 2, mt: 3 }}>
        <StatCard label="Active campaigns" value={metrics.active} helper="Available at checkout" tone="green" icon={<BoltOutlinedIcon />} />
        <StatCard label="Draft campaigns" value={metrics.draft} helper="Ready for review and activation" tone="orange" icon={<LocalOfferOutlinedIcon />} />
        <StatCard label="Voucher redemptions" value={metrics.redemptions} helper="Confirmed promotion usages" tone="blue" icon={<LocalOfferOutlinedIcon />} />
      </Box>

      <Paper className="admin-data-panel" elevation={0} sx={{ mt: 3, border: "1px solid", borderColor: "divider", overflow: "hidden" }}>
        <Stack direction={{ xs: "column", sm: "row" }} alignItems={{ sm: "center" }} justifyContent="space-between" spacing={2} className="panel-summary">
          <Box><Typography fontWeight={900}>Campaign library</Typography><Typography variant="body2" color="text.secondary">Campaign status is enforced by Promotion Service during checkout.</Typography></Box>
          <TextField select label="Status" size="small" value={filter} onChange={(event) => handleFilter(event.target.value)} sx={{ minWidth: 150 }}>
            {statusOptions.map((status) => <MenuItem key={status} value={status}>{status === "ALL" ? "All campaigns" : status}</MenuItem>)}
          </TextField>
        </Stack>
        <Divider />
        {loading ? <Stack alignItems="center" justifyContent="center" sx={{ py: 9 }}><CircularProgress /></Stack> : campaigns.length === 0 ? <Box sx={{ p: 5, textAlign: "center" }}><LocalOfferOutlinedIcon color="disabled" sx={{ fontSize: 42 }} /><Typography fontWeight={800} sx={{ mt: 1 }}>No campaigns found</Typography><Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>Create a campaign, then activate it when it is ready for checkout.</Typography></Box> : <TableContainer><Table><TableHead><TableRow><TableCell>Campaign</TableCell><TableCell>Discount</TableCell><TableCell>Schedule</TableCell><TableCell>Usage</TableCell><TableCell>Status</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead><TableBody>{campaigns.map((campaign) => <TableRow hover key={campaign.id}><TableCell><Stack spacing={0.35}><Stack direction="row" spacing={1} alignItems="center"><Typography className="table-primary">{campaign.name}</Typography><Chip size="small" label={campaign.code} className="soft-chip" /></Stack><Typography className="table-secondary" noWrap>{campaign.description || "No description"}</Typography></Stack></TableCell><TableCell><Typography fontWeight={800}>{campaign.type === "PERCENTAGE" ? `${campaign.discountValue}%` : formatMoney(campaign.discountValue)}</Typography><Typography variant="caption" color="text.secondary">Min. {formatMoney(campaign.minOrderAmount)}{campaign.maxDiscountAmount != null ? ` · Max ${formatMoney(campaign.maxDiscountAmount)}` : ""}</Typography></TableCell><TableCell><Typography variant="body2">{formatDate(campaign.startAt)}</Typography><Typography variant="caption" color="text.secondary">to {formatDate(campaign.endAt)}</Typography></TableCell><TableCell><Typography fontWeight={800}>{campaign.usedCount || 0} / {campaign.usageLimit || "∞"}</Typography><Typography variant="caption" color="text.secondary">Priority {campaign.priority || 0}</Typography></TableCell><TableCell><Chip size="small" color={statusTone[campaign.status] || "default"} label={campaign.status} /></TableCell><TableCell align="right"><Stack direction="row" justifyContent="flex-end"><Tooltip title={campaign.status === "ACTIVE" ? "Deactivate" : "Activate"}><IconButton color={campaign.status === "ACTIVE" ? "success" : "default"} onClick={() => changeStatus(campaign)}>{campaign.status === "ACTIVE" ? <ToggleOnOutlinedIcon /> : <ToggleOffOutlinedIcon />}</IconButton></Tooltip><Tooltip title="Edit"><IconButton onClick={() => openEdit(campaign)}><EditOutlinedIcon /></IconButton></Tooltip><Tooltip title="Delete"><IconButton color="error" onClick={() => remove(campaign)}><DeleteOutlineOutlinedIcon /></IconButton></Tooltip></Stack></TableCell></TableRow>)}</TableBody></Table></TableContainer>}
      </Paper>

      <Dialog open={dialogOpen} onClose={() => !submitting && setDialogOpen(false)} fullWidth maxWidth="md">
        <Box component="form" onSubmit={submit}>
          <DialogTitle>{editing ? "Update promotion campaign" : "Create promotion campaign"}</DialogTitle>
          <DialogContent><Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" }, gap: 2, pt: 1 }}>
            <TextField label="Campaign name" value={form.name} onChange={(event) => setField("name", event.target.value)} required fullWidth />
            <TextField label="Voucher code" value={form.code} onChange={(event) => setField("code", event.target.value.toUpperCase())} required disabled={editing} helperText={editing ? "Voucher code cannot be changed after creation" : "Example: SUMMER20"} fullWidth />
            <TextField select label="Discount type" value={form.type} onChange={(event) => setField("type", event.target.value)} fullWidth>{["PERCENTAGE", "FIXED_AMOUNT", "FREE_SHIPPING"].map((type) => <MenuItem key={type} value={type}>{type}</MenuItem>)}</TextField>
            <TextField label={form.type === "PERCENTAGE" ? "Discount percentage" : "Discount amount"} type="number" inputProps={{ min: 0.01 }} value={form.discountValue} onChange={(event) => setField("discountValue", event.target.value)} required fullWidth />
            <TextField label="Maximum discount (optional)" type="number" inputProps={{ min: 0 }} value={form.maxDiscountAmount} onChange={(event) => setField("maxDiscountAmount", event.target.value)} fullWidth />
            <TextField label="Minimum order amount" type="number" inputProps={{ min: 0 }} value={form.minOrderAmount} onChange={(event) => setField("minOrderAmount", event.target.value)} required fullWidth />
            <TextField label="Start at" type="datetime-local" value={form.startAt} onChange={(event) => setField("startAt", event.target.value)} InputLabelProps={{ shrink: true }} required fullWidth />
            <TextField label="End at" type="datetime-local" value={form.endAt} onChange={(event) => setField("endAt", event.target.value)} InputLabelProps={{ shrink: true }} required fullWidth />
            <TextField label="Usage limit (0 = unlimited)" type="number" inputProps={{ min: 0 }} value={form.usageLimit} onChange={(event) => setField("usageLimit", event.target.value)} required fullWidth />
            <TextField label="Priority" type="number" inputProps={{ min: 0 }} value={form.priority} onChange={(event) => setField("priority", event.target.value)} required fullWidth />
            {editing && <TextField select label="Status" value={form.status} onChange={(event) => setField("status", event.target.value)} fullWidth>{statusOptions.filter((status) => status !== "ALL").map((status) => <MenuItem key={status} value={status}>{status}</MenuItem>)}</TextField>}
            <FormControlLabel control={<Switch checked={form.stackable} onChange={(event) => setField("stackable", event.target.checked)} />} label="Can stack with another campaign" />
            <TextField label="Description" multiline minRows={3} value={form.description} onChange={(event) => setField("description", event.target.value)} sx={{ gridColumn: { sm: "1 / -1" } }} fullWidth />
          </Box></DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}><Button onClick={() => setDialogOpen(false)} disabled={submitting}>Cancel</Button><Button type="submit" variant="contained" disabled={submitting}>{submitting ? "Saving..." : editing ? "Save changes" : "Create draft"}</Button></DialogActions>
        </Box>
      </Dialog>
    </MainLayout>
  );
}
