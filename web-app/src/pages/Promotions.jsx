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
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import FolderSpecialOutlinedIcon from "@mui/icons-material/FolderSpecialOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import ToggleOffOutlinedIcon from "@mui/icons-material/ToggleOffOutlined";
import ToggleOnOutlinedIcon from "@mui/icons-material/ToggleOnOutlined";
import { useCallback, useEffect, useMemo, useState } from "react";
import { PageHeader, StatCard } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import {
  createPromotion,
  deletePromotion,
  getPromotions,
  updatePromotion,
} from "../services/promotionService";
import { getOrdersByPromotionCode } from "../services/orderService";

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
  return `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 }).format(Number(value || 0))} ₫`;
}

function formatDate(value) {
  return value ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "—";
}

function promotionPayload(form, includeStatus) {
  const payload = {
    name: form.name.trim(),
    description: form.description?.trim() || null,
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
  const [ordersDialogOpen, setOrdersDialogOpen] = useState(false);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [selectedPromotion, setSelectedPromotion] = useState(null);
  const [promotionOrders, setPromotionOrders] = useState([]);
  const [promotionOrderCount, setPromotionOrderCount] = useState(0);

  const editing = Boolean(form.id);

  const loadCampaigns = useCallback(async (status = filter) => {
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
  }, [filter]);

  useEffect(() => { loadCampaigns(); }, [loadCampaigns]);

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

  const openPromotionOrders = async (campaign) => {
    setSelectedPromotion(campaign);
    setPromotionOrders([]);
    setPromotionOrderCount(0);
    setOrdersDialogOpen(true);
    setOrdersLoading(true);
    try {
      const data = await getOrdersByPromotionCode(campaign.code, { page: 1, size: 50 });
      setPromotionOrders(data.content || []);
      setPromotionOrderCount(data.totalElements || 0);
    } catch (error) {
      setOrdersDialogOpen(false);
      setErrorMessage(error.response?.data?.message || "Could not load orders using this promotion.");
    } finally {
      setOrdersLoading(false);
    }
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

      <Box className="promotions-metrics-grid" sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "repeat(3, 1fr)" }, gap: 2, mt: 3 }}>
        <StatCard label="Active campaigns" value={metrics.active} helper="Available at checkout" tone="green" icon={<BoltOutlinedIcon />} />
        <StatCard label="Draft campaigns" value={metrics.draft} helper="Ready for review and activation" tone="zinc" icon={<LocalOfferOutlinedIcon />} />
        <StatCard label="Voucher redemptions" value={metrics.redemptions} helper="Confirmed promotion usages" tone="zinc" icon={<LocalOfferOutlinedIcon />} />
      </Box>

      <Paper className="admin-data-panel promotion-library-panel" elevation={0} sx={{ mt: 3, border: "1px solid", borderColor: "divider", overflow: "hidden" }}>
        <Stack direction={{ xs: "column", sm: "row" }} alignItems={{ sm: "center" }} justifyContent="space-between" spacing={2} className="panel-summary">
          <Stack direction="row" spacing={1.25} alignItems="center" className="promotion-library-heading">
            <Box className="promotion-library-icon"><FolderSpecialOutlinedIcon /></Box>
            <Box>
              <Typography className="promotion-library-title">Campaign library</Typography>
              <Typography className="promotion-library-description">Campaign status is enforced by Promotion Service during checkout.</Typography>
            </Box>
          </Stack>
          <TextField select label="Status" size="small" value={filter} onChange={(event) => handleFilter(event.target.value)} sx={{ minWidth: 150 }}>
            {statusOptions.map((status) => <MenuItem key={status} value={status}>{status === "ALL" ? "All campaigns" : status}</MenuItem>)}
          </TextField>
        </Stack>
        <Divider />
        {loading ? <Stack alignItems="center" justifyContent="center" sx={{ py: 9 }}><CircularProgress /></Stack> : campaigns.length === 0 ? <Box sx={{ p: 5, textAlign: "center" }}><LocalOfferOutlinedIcon color="disabled" sx={{ fontSize: 42 }} /><Typography fontWeight={800} sx={{ mt: 1 }}>No campaigns found</Typography><Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>Create a campaign, then activate it when it is ready for checkout.</Typography></Box> : <>
          <TableContainer><Table><TableHead><TableRow><TableCell>Campaign</TableCell><TableCell>Discount</TableCell><TableCell>Schedule</TableCell><TableCell>Usage</TableCell><TableCell>Status</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead><TableBody>{campaigns.map((campaign) => <TableRow hover key={campaign.id}><TableCell><Stack spacing={0.35}><Typography className="table-primary">{campaign.name}</Typography><Typography className="promotion-code">{campaign.code}</Typography><Typography className="table-secondary" noWrap>{campaign.description || "No description"}</Typography></Stack></TableCell><TableCell><Typography fontWeight={800}>{campaign.type === "PERCENTAGE" ? `${campaign.discountValue}%` : formatMoney(campaign.discountValue)}</Typography><Typography variant="caption" color="text.secondary">Min. {formatMoney(campaign.minOrderAmount)}{campaign.maxDiscountAmount != null ? ` · Max ${formatMoney(campaign.maxDiscountAmount)}` : ""}</Typography></TableCell><TableCell><Typography variant="body2">{formatDate(campaign.startAt)}</Typography><Typography variant="caption" color="text.secondary">to {formatDate(campaign.endAt)}</Typography></TableCell><TableCell><Typography fontWeight={800}>{campaign.usedCount || 0} / {campaign.usageLimit || "∞"}</Typography><Typography variant="caption" color="text.secondary">Priority {campaign.priority || 0}</Typography></TableCell><TableCell><Chip size="small" color={statusTone[campaign.status] || "default"} label={campaign.status} /></TableCell><TableCell align="right"><Stack direction="row" justifyContent="flex-end" className="promotion-row-actions"><Tooltip title={campaign.status === "ACTIVE" ? "Deactivate" : "Activate"}><IconButton className="promotion-action-button" onClick={() => changeStatus(campaign)}>{campaign.status === "ACTIVE" ? <ToggleOnOutlinedIcon /> : <ToggleOffOutlinedIcon />}</IconButton></Tooltip><Tooltip title="View used orders"><IconButton className="promotion-action-button" onClick={() => openPromotionOrders(campaign)}><VisibilityOutlinedIcon /></IconButton></Tooltip><Tooltip title="Edit"><IconButton className="promotion-action-button" onClick={() => openEdit(campaign)}><EditOutlinedIcon /></IconButton></Tooltip><Tooltip title="Delete"><IconButton className="promotion-action-button promotion-delete-action" onClick={() => remove(campaign)}><DeleteOutlineOutlinedIcon /></IconButton></Tooltip></Stack></TableCell></TableRow>)}</TableBody></Table></TableContainer>
          {campaigns.length <= 1 && <Box className="promotion-library-helper"><Box><Typography className="promotion-library-helper-title">Thư viện chiến dịch đang gọn gàng</Typography><Typography className="promotion-library-helper-text">Tạo thêm chiến dịch để chuẩn bị cho các đợt ưu đãi sắp tới.</Typography></Box><Button variant="outlined" size="small" startIcon={<AddOutlinedIcon />} onClick={openCreate}>Tạo chiến dịch</Button></Box>}
        </>}
      </Paper>

      <Dialog open={ordersDialogOpen} onClose={() => setOrdersDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>
          Orders using {selectedPromotion?.code || "promotion"}
        </DialogTitle>
        <DialogContent dividers>
          {ordersLoading ? <Stack alignItems="center" sx={{ py: 6 }}><CircularProgress /></Stack> : promotionOrders.length === 0 ? <Typography color="text.secondary" sx={{ py: 4, textAlign: "center" }}>No orders have used this promotion yet.</Typography> : <>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{promotionOrderCount} order(s) found</Typography>
            <TableContainer component={Paper} variant="outlined" elevation={0}><Table size="small"><TableHead><TableRow><TableCell>Order</TableCell><TableCell>User</TableCell><TableCell>Status</TableCell><TableCell align="right">Discount</TableCell><TableCell align="right">Total</TableCell><TableCell>Created</TableCell></TableRow></TableHead><TableBody>{promotionOrders.map((order) => <TableRow key={order.id}><TableCell><Typography fontWeight={800}>{order.orderCode || order.id}</Typography></TableCell><TableCell>{order.userId || "-"}</TableCell><TableCell><Chip size="small" label={order.status || "UNKNOWN"} /></TableCell><TableCell align="right" sx={{ color: "success.main", fontWeight: 800 }}>-{formatMoney(order.discountAmount)}</TableCell><TableCell align="right">{formatMoney(order.totalAmount)}</TableCell><TableCell>{formatDate(order.createdAt)}</TableCell></TableRow>)}</TableBody></Table></TableContainer>
          </>}
        </DialogContent>
        <DialogActions><Button onClick={() => setOrdersDialogOpen(false)}>Close</Button></DialogActions>
      </Dialog>

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
