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
  IconButton,
  MenuItem,
  Pagination,
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
import AssignmentReturnOutlinedIcon from "@mui/icons-material/AssignmentReturnOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import LocationOnOutlinedIcon from "@mui/icons-material/LocationOnOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import RouteOutlinedIcon from "@mui/icons-material/RouteOutlined";
import SettingsSuggestOutlinedIcon from "@mui/icons-material/SettingsSuggestOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { useCallback, useEffect, useMemo, useState } from "react";
import { PageHeader, StatCard } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { hasAdminRole } from "../services/authenticationService";
import {
  assignShipmentCarrier,
  getShipments,
  updateShipmentState,
} from "../services/shippingService";
import { formatDateTime } from "../utils/dateTimeUtils";
import "./Shipments.css";

const statuses = [
  "ALL",
  "CREATED",
  "PACKING",
  "READY_TO_SHIP",
  "IN_TRANSIT",
  "DELIVERED",
  "DELIVERY_FAILED",
  "RETURNING",
  "RETURNED",
  "CANCELLED",
];

const statusMeta = {
  CREATED: { label: "Created", color: "default", icon: <Inventory2OutlinedIcon /> },
  PACKING: { label: "Packing", color: "info", icon: <Inventory2OutlinedIcon /> },
  READY_TO_SHIP: { label: "Ready to ship", color: "primary", icon: <LocalShippingOutlinedIcon /> },
  IN_TRANSIT: { label: "In transit", color: "secondary", icon: <RouteOutlinedIcon /> },
  DELIVERED: { label: "Delivered", color: "success", icon: <CheckCircleOutlineOutlinedIcon /> },
  DELIVERY_FAILED: { label: "Delivery failed", color: "error", icon: <ErrorOutlineOutlinedIcon /> },
  RETURNING: { label: "Returning", color: "warning", icon: <AssignmentReturnOutlinedIcon /> },
  RETURNED: { label: "Returned", color: "warning", icon: <AssignmentReturnOutlinedIcon /> },
  CANCELLED: { label: "Cancelled", color: "default", icon: <ErrorOutlineOutlinedIcon /> },
};

const transitions = {
  CREATED: [{ action: "packing", label: "Start packing" }],
  PACKING: [{ action: "ready-to-ship", label: "Ready to ship" }],
  READY_TO_SHIP: [{ action: "ship", label: "Hand to carrier" }],
  IN_TRANSIT: [
    { action: "deliver", label: "Mark delivered", tone: "success" },
    { action: "delivery-failed", label: "Delivery failed", tone: "error" },
  ],
  DELIVERY_FAILED: [
    { action: "ship", label: "Retry delivery" },
    { action: "returning", label: "Return to sender", tone: "warning" },
  ],
  RETURNING: [{ action: "returned", label: "Mark returned", tone: "warning" }],
};

const cancellable = new Set(["CREATED", "PACKING", "READY_TO_SHIP"]);
const carrierEditable = new Set(["CREATED", "PACKING", "READY_TO_SHIP"]);
const carrierInitial = { carrier: "", trackingNumber: "", estimatedDeliveryAt: "" };

const shortId = (value) => value ? `${value.slice(0, 8)}…${value.slice(-4)}` : "-";
const formatDate = (value) => value ? formatDateTime(value) : "Not scheduled";
const localDateTime = (value) => value ? new Date(value).toISOString().slice(0, 16) : "";

export default function Shipments({ shipperMode = false }) {
  const [shipments, setShipments] = useState([]);
  const [status, setStatus] = useState("ALL");
  const [page, setPage] = useState(0);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [selected, setSelected] = useState(null);
  const [carrierOpen, setCarrierOpen] = useState(false);
  const [carrierForm, setCarrierForm] = useState(carrierInitial);
  const [saving, setSaving] = useState(false);
  const isAdmin = hasAdminRole();
  const canManageOperations = isAdmin && !shipperMode;

  const load = useCallback(async (nextPage = page, nextStatus = status) => {
    setLoading(true);
    setError("");
    try {
      const data = await getShipments({ status: nextStatus, page: nextPage, size: 12 });
      setShipments(data.content || []);
      setPageInfo({ totalPages: data.totalPages || 0, totalElements: data.totalElements || 0 });
      setPage(data.number ?? nextPage);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Could not load shipments. Ensure Shipping Service is running.");
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    load(0, status);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  const metrics = useMemo(() => ({
    active: shipments.filter((item) => ["PACKING", "READY_TO_SHIP", "IN_TRANSIT"].includes(item.status)).length,
    transit: shipments.filter((item) => item.status === "IN_TRANSIT").length,
    attention: shipments.filter((item) => ["DELIVERY_FAILED", "RETURNING"].includes(item.status)).length,
    delivered: shipments.filter((item) => item.status === "DELIVERED").length,
  }), [shipments]);

  const updateSelected = (updated) => {
    setSelected(updated);
    setShipments((items) => items.map((item) => item.id === updated.id ? updated : item));
  };

  const runTransition = async (transition) => {
    if (!selected || saving) return;
    if (transition.action === "ship" && (!selected.carrier || !selected.trackingNumber)) {
      setError("Assign a carrier and tracking number before handing this shipment to the carrier.");
      openCarrier(selected);
      return;
    }
    if (["delivery-failed", "returning", "cancel"].includes(transition.action)
      && !window.confirm(`${transition.label} for order ${shortId(selected.orderId)}?`)) return;

    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const updated = await updateShipmentState(selected.id, transition.action, {
        description: transition.label,
        location: "",
      });
      updateSelected(updated);
      setSuccess(`Shipment updated to ${statusMeta[updated.status]?.label || updated.status}.`);
      await load(page, status);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Could not update shipment status.");
    } finally {
      setSaving(false);
    }
  };

  const openCarrier = (shipment) => {
    setSelected(shipment);
    setCarrierForm({
      carrier: shipment.carrier || "",
      trackingNumber: shipment.trackingNumber || "",
      estimatedDeliveryAt: localDateTime(shipment.estimatedDeliveryAt),
    });
    setCarrierOpen(true);
  };

  const saveCarrier = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const updated = await assignShipmentCarrier(selected.id, {
        carrier: carrierForm.carrier.trim(),
        trackingNumber: carrierForm.trackingNumber.trim(),
        estimatedDeliveryAt: carrierForm.estimatedDeliveryAt
          ? new Date(carrierForm.estimatedDeliveryAt).toISOString()
          : null,
      });
      updateSelected(updated);
      setCarrierOpen(false);
      setSuccess("Carrier information saved successfully.");
      await load(page, status);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Could not assign carrier.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <MainLayout>
      <PageHeader
        eyebrow={shipperMode ? "Delivery · Shipper workspace" : "Fulfillment · Shipping service"}
        title="Shipments"
        description={shipperMode ? "View all shipments and confirm orders that have been delivered." : "Coordinate packing, carrier handoff, delivery attempts and returns from one operational view."}
        actions={<Button variant="outlined" startIcon={<RefreshOutlinedIcon />} onClick={() => load(page, status)} disabled={loading}>Refresh</Button>}
      />

      {error && <Alert severity="error" sx={{ mt: 2 }} onClose={() => setError("")}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mt: 2 }} onClose={() => setSuccess("")}>{success}</Alert>}

      <Box className="shipment-metrics">
        <StatCard label="Active pipeline" value={metrics.active} helper="Packing through transit" icon={<LocalShippingOutlinedIcon />} />
        <StatCard label="In transit" value={metrics.transit} helper="Currently with carriers" tone="blue" icon={<RouteOutlinedIcon />} />
        <StatCard label="Needs attention" value={metrics.attention} helper="Failed or returning" tone="orange" icon={<ErrorOutlineOutlinedIcon />} />
        <StatCard label="Delivered" value={metrics.delivered} helper="On this loaded page" tone="green" icon={<CheckCircleOutlineOutlinedIcon />} />
      </Box>

      <Paper className="admin-data-panel shipment-panel" elevation={0}>
        <Stack direction={{ xs: "column", sm: "row" }} alignItems={{ sm: "center" }} justifyContent="space-between" spacing={2} className="panel-summary">
          <Stack direction="row" spacing={1.25} alignItems="center">
            <Box className="shipment-library-icon"><LocalShippingOutlinedIcon /></Box>
            <Box>
              <Typography className="section-heading-title">Fulfillment queue</Typography>
              <Typography className="section-heading-description">{pageInfo.totalElements} shipments tracked across all delivery states.</Typography>
            </Box>
          </Stack>
          <TextField select label="Status" size="small" value={status} onChange={(event) => setStatus(event.target.value)} sx={{ minWidth: 185 }}>
            {statuses.map((item) => <MenuItem key={item} value={item}>{item === "ALL" ? "All shipments" : statusMeta[item]?.label}</MenuItem>)}
          </TextField>
        </Stack>
        <Divider />

        {loading ? <Stack alignItems="center" sx={{ py: 9 }}><CircularProgress /></Stack> : shipments.length === 0 ? (
          <Box className="shipment-empty"><LocalShippingOutlinedIcon /><Typography fontWeight={850}>No shipments found</Typography><Typography color="text.secondary">Confirmed orders will appear here automatically.</Typography></Box>
        ) : (
          <TableContainer>
            <Table className="shipment-table">
              <TableHead><TableRow><TableCell>Shipment</TableCell><TableCell>Order & customer</TableCell><TableCell>Carrier</TableCell><TableCell>Delivery window</TableCell><TableCell>Status</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead>
              <TableBody>{shipments.map((shipment) => {
                const meta = statusMeta[shipment.status] || statusMeta.CREATED;
                return <TableRow hover key={shipment.id}>
                  <TableCell><Stack direction="row" spacing={1.25} alignItems="center"><Box className={`shipment-status-icon shipment-status-${shipment.status.toLowerCase()}`}>{meta.icon}</Box><Box><Typography className="table-primary">{shortId(shipment.id)}</Typography><Typography className="table-secondary">Created {formatDate(shipment.createdAt)}</Typography></Box></Stack></TableCell>
                  <TableCell><Typography className="table-primary">Order {shortId(shipment.orderId)}</Typography><Typography className="entity-id">User {shortId(shipment.userId)}</Typography></TableCell>
                  <TableCell>{shipment.carrier ? <><Typography className="table-primary">{shipment.carrier}</Typography><Typography className="shipment-tracking">{shipment.trackingNumber}</Typography></> : <Chip size="small" variant="outlined" label="Not assigned" />}</TableCell>
                  <TableCell><Typography className="table-primary">{formatDate(shipment.estimatedDeliveryAt)}</Typography><Typography className="table-secondary">{shipment.deliveredAt ? `Delivered ${formatDate(shipment.deliveredAt)}` : shipment.shippedAt ? `Shipped ${formatDate(shipment.shippedAt)}` : "Awaiting handoff"}</Typography></TableCell>
                  <TableCell><Chip size="small" color={meta.color} label={meta.label} /></TableCell>
                  <TableCell align="right"><Stack direction="row" justifyContent="flex-end" className="row-actions">
                    {canManageOperations && carrierEditable.has(shipment.status) && <Tooltip title="Assign carrier"><IconButton onClick={() => openCarrier(shipment)}><SettingsSuggestOutlinedIcon /></IconButton></Tooltip>}
                    <Tooltip title="View and manage"><IconButton onClick={() => setSelected(shipment)}><VisibilityOutlinedIcon /></IconButton></Tooltip>
                  </Stack></TableCell>
                </TableRow>;
              })}</TableBody>
            </Table>
          </TableContainer>
        )}

        {pageInfo.totalPages > 1 && <Box className="shipment-pagination"><Typography variant="body2" color="text.secondary">Page {page + 1} of {pageInfo.totalPages}</Typography><Pagination page={page + 1} count={pageInfo.totalPages} onChange={(_, value) => load(value - 1, status)} /></Box>}
      </Paper>

      <Dialog open={Boolean(selected) && !carrierOpen} onClose={() => !saving && setSelected(null)} fullWidth maxWidth="md">
        {selected && <>
          <DialogTitle><Stack direction="row" alignItems="center" justifyContent="space-between" gap={2}><Box><Typography variant="overline" color="primary">Shipment detail</Typography><Typography variant="h6" fontWeight={850}>Order {shortId(selected.orderId)}</Typography></Box><Chip color={statusMeta[selected.status]?.color || "default"} label={statusMeta[selected.status]?.label || selected.status} /></Stack></DialogTitle>
          <DialogContent dividers>
            <Box className="shipment-detail-grid">
              <Box className="shipment-detail-card"><Typography variant="overline">Destination</Typography><Stack direction="row" spacing={1} sx={{ mt: 1 }}><LocationOnOutlinedIcon color="primary" /><Typography>{selected.shippingAddress}</Typography></Stack></Box>
              <Box className="shipment-detail-card"><Typography variant="overline">Carrier</Typography><Typography fontWeight={850} sx={{ mt: 1 }}>{selected.carrier || "Not assigned"}</Typography><Typography className="shipment-tracking">{selected.trackingNumber || "Add carrier details before shipping"}</Typography></Box>
            </Box>

            <Typography className="shipment-timeline-title">Shipment timeline</Typography>
            <Box className="shipment-timeline">{(selected.timeline || []).map((event, index) => <Box className="shipment-timeline-item" key={event.id || `${event.status}-${index}`}><Box className="shipment-timeline-dot" /><Box><Stack direction="row" spacing={1} alignItems="center"><Typography fontWeight={850}>{statusMeta[event.status]?.label || event.status}</Typography><Typography variant="caption" color="text.secondary">{formatDate(event.occurredAt)}</Typography></Stack><Typography variant="body2" color="text.secondary">{event.description || "Status updated"}{event.location ? ` · ${event.location}` : ""}</Typography></Box></Box>)}</Box>
          </DialogContent>
          <DialogActions className="shipment-dialog-actions">
            <Button onClick={() => setSelected(null)} disabled={saving}>Close</Button>
            {canManageOperations && carrierEditable.has(selected.status) && <Button variant="outlined" onClick={() => openCarrier(selected)} disabled={saving}>Carrier details</Button>}
            {canManageOperations && cancellable.has(selected.status) && <Button color="error" onClick={() => runTransition({ action: "cancel", label: "Cancel shipment" })} disabled={saving}>Cancel shipment</Button>}
            {(shipperMode ? (selected.status === "IN_TRANSIT" ? [{ action: "deliver", label: "Mark delivered", tone: "success" }] : []) : (transitions[selected.status] || [])).map((transition) => <Button key={transition.action} variant="contained" color={transition.tone || "primary"} onClick={() => runTransition(transition)} disabled={saving}>{transition.label}</Button>)}
          </DialogActions>
        </>}
      </Dialog>

      <Dialog open={carrierOpen} onClose={() => !saving && setCarrierOpen(false)} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={saveCarrier}>
          <DialogTitle>Carrier assignment</DialogTitle>
          <DialogContent dividers><Stack spacing={2} sx={{ pt: 0.5 }}>
            <TextField label="Carrier" placeholder="GHN, GHTK, Viettel Post..." value={carrierForm.carrier} onChange={(event) => setCarrierForm((form) => ({ ...form, carrier: event.target.value }))} required fullWidth />
            <TextField label="Tracking number" value={carrierForm.trackingNumber} onChange={(event) => setCarrierForm((form) => ({ ...form, trackingNumber: event.target.value }))} required fullWidth />
            <TextField label="Estimated delivery" type="datetime-local" value={carrierForm.estimatedDeliveryAt} onChange={(event) => setCarrierForm((form) => ({ ...form, estimatedDeliveryAt: event.target.value }))} InputLabelProps={{ shrink: true }} fullWidth />
          </Stack></DialogContent>
          <DialogActions><Button onClick={() => setCarrierOpen(false)} disabled={saving}>Cancel</Button><Button type="submit" variant="contained" disabled={saving}>{saving ? "Saving..." : "Save carrier"}</Button></DialogActions>
        </Box>
      </Dialog>
    </MainLayout>
  );
}
