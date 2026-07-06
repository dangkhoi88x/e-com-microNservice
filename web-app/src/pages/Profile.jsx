import {
  Alert,
  Avatar,
  Box,
  Button,
  CircularProgress,
  Divider,
  Grid,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AccountCircleOutlinedIcon from "@mui/icons-material/AccountCircleOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import { useEffect, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import { getMyProfile, updateMyProfile } from "../services/profileService";

const emptyProfile = {
  userId: "",
  firstName: "",
  lastName: "",
  avatarUrl: "",
  bio: "",
  birthDate: "",
};

export default function Profile() {
  const [profile, setProfile] = useState(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const fullName = [profile.firstName, profile.lastName]
    .filter(Boolean)
    .join(" ");

  const loadProfile = async () => {
    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const data = await getMyProfile();
      setProfile({
        userId: data?.userId || "",
        firstName: data?.firstName || "",
        lastName: data?.lastName || "",
        avatarUrl: data?.avatarUrl || "",
        bio: data?.bio || "",
        birthDate: data?.birthDate || "",
      });
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not load profile.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, []);

  const setField = (field) => (event) => {
    setProfile((current) => ({
      ...current,
      [field]: event.target.value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");

    const payload = {
      firstName: profile.firstName.trim(),
      lastName: profile.lastName.trim(),
      avatarUrl: profile.avatarUrl.trim(),
      bio: profile.bio.trim(),
      birthDate: profile.birthDate || null,
    };

    try {
      const data = await updateMyProfile(payload);
      setProfile({
        userId: data?.userId || profile.userId,
        firstName: data?.firstName || "",
        lastName: data?.lastName || "",
        avatarUrl: data?.avatarUrl || "",
        bio: data?.bio || "",
        birthDate: data?.birthDate || "",
      });
      setSuccessMessage("Profile updated successfully.");
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not update profile.",
      );
    } finally {
      setSubmitting(false);
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
            Profile
          </Typography>
          <Typography color="text.secondary">
            Review and update current user profile.
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshOutlinedIcon />}
          onClick={loadProfile}
        >
          Refresh
        </Button>
      </Stack>

      {loading ? (
        <Paper
          elevation={0}
          sx={{
            mt: 3,
            p: 6,
            border: "1px solid",
            borderColor: "divider",
          }}
        >
          <Stack alignItems="center" spacing={2}>
            <CircularProgress />
            <Typography color="text.secondary">Loading profile...</Typography>
          </Stack>
        </Paper>
      ) : (
        <Grid container spacing={3} sx={{ mt: 0.5 }}>
          <Grid item xs={12} md={4}>
            <Paper
              elevation={0}
              sx={{ p: 3, border: "1px solid", borderColor: "divider" }}
            >
              <Stack alignItems="center" spacing={2}>
                <Avatar
                  src={profile.avatarUrl || undefined}
                  sx={{ width: 96, height: 96, bgcolor: "primary.main" }}
                >
                  <AccountCircleOutlinedIcon sx={{ fontSize: 56 }} />
                </Avatar>
                <Box sx={{ textAlign: "center" }}>
                  <Typography variant="h6" fontWeight={900}>
                    {fullName || "Unnamed user"}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {profile.userId}
                  </Typography>
                </Box>
              </Stack>

              <Divider sx={{ my: 2 }} />

              <Stack spacing={1.25}>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Birth date
                  </Typography>
                  <Typography fontWeight={800}>
                    {profile.birthDate || "-"}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Bio
                  </Typography>
                  <Typography fontWeight={700}>
                    {profile.bio || "-"}
                  </Typography>
                </Box>
              </Stack>
            </Paper>
          </Grid>

          <Grid item xs={12} md={8}>
            <Paper
              elevation={0}
              sx={{ p: 3, border: "1px solid", borderColor: "divider" }}
            >
              {errorMessage && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {errorMessage}
                </Alert>
              )}
              {successMessage && (
                <Alert severity="success" sx={{ mb: 2 }}>
                  {successMessage}
                </Alert>
              )}

              <Box component="form" onSubmit={handleSubmit}>
                <Grid container spacing={2.5}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="First name"
                      value={profile.firstName}
                      onChange={setField("firstName")}
                      fullWidth
                      required
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Last name"
                      value={profile.lastName}
                      onChange={setField("lastName")}
                      fullWidth
                      required
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      label="Avatar URL"
                      value={profile.avatarUrl}
                      onChange={setField("avatarUrl")}
                      fullWidth
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Birth date"
                      type="date"
                      value={profile.birthDate}
                      onChange={setField("birthDate")}
                      fullWidth
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      label="Bio"
                      value={profile.bio}
                      onChange={setField("bio")}
                      fullWidth
                      multiline
                      minRows={4}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Stack direction="row" justifyContent="flex-end">
                      <Button
                        type="submit"
                        variant="contained"
                        startIcon={<SaveOutlinedIcon />}
                        disabled={submitting}
                      >
                        {submitting ? "Saving..." : "Save profile"}
                      </Button>
                    </Stack>
                  </Grid>
                </Grid>
              </Box>
            </Paper>
          </Grid>
        </Grid>
      )}
    </MainLayout>
  );
}
