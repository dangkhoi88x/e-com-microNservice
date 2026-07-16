import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import WavingHandOutlinedIcon from "@mui/icons-material/WavingHandOutlined";
import { useEffect, useMemo, useState } from "react";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { hasAnyRole } from "../services/authenticationService";
import {
  getAdminNotifications,
  getMyNotifications,
} from "../services/notificationService";
import { formatDateTime, formatRelativeTime } from "../utils/dateTimeUtils";

const notificationConfig = {
  USER_CREATED: {
    label: "Welcome",
    color: "success",
    icon: <WavingHandOutlinedIcon />,
  },
  ORDER_CREATED: {
    label: "Order created",
    color: "primary",
    icon: <CheckCircleOutlineOutlinedIcon />,
  },
  ORDER_CANCELLED: {
    label: "Order cancelled",
    color: "default",
    icon: <CancelOutlinedIcon />,
  },
  ORDER_STATUS_UPDATED: {
    label: "Status updated",
    color: "info",
    icon: <LocalShippingOutlinedIcon />,
  },
  SYSTEM: {
    label: "System",
    color: "warning",
    icon: <NotificationsOutlinedIcon />,
  },
};

const getNotificationConfig = (type) =>
  notificationConfig[type] || {
    label: type || "Notification",
    color: "default",
    icon: <NotificationsOutlinedIcon />,
  };

export default function Notifications() {
  const isAdmin = hasAnyRole("ROLE_ADMIN", "ADMIN");
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  const unreadCount = useMemo(
    () => notifications.filter((notification) => !notification.read).length,
    [notifications],
  );

  const loadNotifications = async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const data = isAdmin ? await getAdminNotifications() : await getMyNotifications();
      setNotifications(data);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not load notifications.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Account"
        title="Notifications"
        description={
          isAdmin
            ? "Track all order and system notifications for the store."
            : "Track account, order, and system notifications."
        }
        actions={
        <Button
          variant="outlined"
          startIcon={<RefreshOutlinedIcon />}
          onClick={loadNotifications}
        >
          Refresh
        </Button>
        }
      />

      <Paper
        elevation={0}
        sx={{
          mt: 3,
          border: "1px solid",
          borderColor: "divider",
          overflow: "hidden",
        }}
      >
        <Stack
          direction={{ xs: "column", sm: "row" }}
          justifyContent="space-between"
          alignItems={{ xs: "stretch", sm: "center" }}
          spacing={2}
          sx={{ p: 2.5 }}
        >
          <Box>
            <Typography fontWeight={900}>Inbox</Typography>
            <Typography variant="body2" color="text.secondary">
              {isAdmin ? "Admin inbox" : "My inbox"} · {notifications.length} notifications
            </Typography>
          </Box>
          <Chip
            label={`${unreadCount} unread`}
            color={unreadCount > 0 ? "primary" : "default"}
            variant={unreadCount > 0 ? "filled" : "outlined"}
          />
        </Stack>

        <Divider />

        {errorMessage && (
          <Alert severity="error" sx={{ m: 2.5 }}>
            {errorMessage}
          </Alert>
        )}

        {loading ? (
          <Stack alignItems="center" justifyContent="center" sx={{ py: 8 }}>
            <CircularProgress />
            <Typography sx={{ mt: 2 }} color="text.secondary">
              Loading notifications...
            </Typography>
          </Stack>
        ) : notifications.length === 0 ? (
          <Stack alignItems="center" justifyContent="center" sx={{ py: 8, px: 3 }}>
            <NotificationsOutlinedIcon color="disabled" sx={{ fontSize: 54 }} />
            <Typography variant="h6" fontWeight={900} sx={{ mt: 2 }}>
              No notifications yet
            </Typography>
            <Typography color="text.secondary" textAlign="center">
              Create, cancel, or update an order to see notifications here.
            </Typography>
          </Stack>
        ) : (
          <List disablePadding>
            {notifications.map((notification, index) => {
              const config = getNotificationConfig(notification.type);

              return (
                <Box key={notification.id || `${notification.type}-${index}`}>
                  <ListItem
                    alignItems="flex-start"
                    sx={{
                      px: 2.5,
                      py: 2,
                      bgcolor: notification.read
                        ? "background.paper"
                        : "action.hover",
                    }}
                  >
                    <ListItemAvatar>
                      <Box
                        sx={{
                          width: 44,
                          height: 44,
                          borderRadius: "50%",
                          display: "grid",
                          placeItems: "center",
                          color: `${config.color}.main`,
                          bgcolor: "action.hover",
                          border: "1px solid",
                          borderColor: "divider",
                        }}
                      >
                        {config.icon}
                      </Box>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Stack
                          direction={{ xs: "column", sm: "row" }}
                          alignItems={{ xs: "flex-start", sm: "center" }}
                          justifyContent="space-between"
                          spacing={1}
                        >
                          <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={900}>
                              {notification.title || config.label}
                            </Typography>
                            <Chip
                              size="small"
                              label={config.label}
                              color={config.color}
                              variant="outlined"
                            />
                          </Stack>
                          <Typography variant="body2" color="text.secondary">
                            {formatRelativeTime(notification.createdAt)}
                          </Typography>
                        </Stack>
                      }
                      secondary={
                        <Box sx={{ mt: 0.75 }}>
                          <Typography color="text.primary">
                            {notification.message || "-"}
                          </Typography>
                          {isAdmin && notification.userId && (
                            <Typography variant="caption" color="text.secondary">
                              User: {notification.userId}
                            </Typography>
                          )}
                          <Typography variant="caption" color="text.secondary">
                            {formatDateTime(notification.createdAt)}
                          </Typography>
                        </Box>
                      }
                    />
                  </ListItem>
                  {index < notifications.length - 1 && <Divider component="li" />}
                </Box>
              );
            })}
          </List>
        )}
      </Paper>
    </MainLayout>
  );
}
