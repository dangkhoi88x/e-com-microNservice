import { Paper, Typography } from "@mui/material";
import MainLayout from "../layouts/MainLayout";

export default function Profile() {
  return (
    <MainLayout>
      <Typography variant="h4" fontWeight={900}>
        Profile
      </Typography>
      <Typography color="text.secondary">
        Review and update current user profile.
      </Typography>

      <Paper
        elevation={0}
        sx={{ mt: 3, p: 3, border: "1px solid", borderColor: "divider" }}
      >
        <Typography fontWeight={800}>Profile details will be connected next.</Typography>
      </Paper>
    </MainLayout>
  );
}
