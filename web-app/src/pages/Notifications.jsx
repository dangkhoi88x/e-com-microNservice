import { Paper, Typography } from "@mui/material";
import MainLayout from "../layouts/MainLayout";

export default function Notifications() {
  return (
    <MainLayout>
      <Typography variant="h4" fontWeight={900}>
        Notifications
      </Typography>
      <Typography color="text.secondary">
        Track account and system notifications.
      </Typography>

      <Paper
        elevation={0}
        sx={{ mt: 3, p: 3, border: "1px solid", borderColor: "divider" }}
      >
        <Typography fontWeight={800}>Notifications will be connected next.</Typography>
      </Paper>
    </MainLayout>
  );
}
