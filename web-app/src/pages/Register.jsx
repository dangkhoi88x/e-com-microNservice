import {
  Alert,
  Box,
  Button,
  CircularProgress,
  InputAdornment,
  Paper,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import BadgeOutlinedIcon from "@mui/icons-material/BadgeOutlined";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import PersonAddAltOutlinedIcon from "@mui/icons-material/PersonAddAltOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import heroImage from "../assets/hero.png";
import { register } from "../services/authenticationService";

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
  });
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const setField = (field) => (event) => {
    setForm((current) => ({
      ...current,
      [field]: event.target.value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      await register({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      setSuccessMessage("Account created. Please log in.");
      setTimeout(() => navigate("/login", { replace: true }), 900);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message ||
          "Register failed. Please check your information.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "grid",
        gridTemplateColumns: { xs: "1fr", md: "0.95fr 1.05fr" },
        bgcolor: "#f6f8fc",
      }}
    >
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          px: { xs: 2, sm: 4 },
          py: 5,
        }}
      >
        <Paper
          elevation={0}
          sx={{
            width: "100%",
            maxWidth: 480,
            p: { xs: 3, sm: 4 },
            border: "1px solid",
            borderColor: "divider",
            boxShadow: "0 24px 70px rgba(15, 23, 42, 0.10)",
          }}
        >
          <Stack spacing={0.75} sx={{ mb: 3 }}>
            <Typography variant="overline" color="primary" fontWeight={800}>
              Create account
            </Typography>
            <Typography variant="h4" fontWeight={900}>
              Register
            </Typography>
            <Typography color="text.secondary">
              Create a user account, then sign in to access the workspace.
            </Typography>
          </Stack>

          <Box component="form" onSubmit={handleSubmit}>
            <Stack spacing={2.25}>
              <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                <TextField
                  label="First name"
                  value={form.firstName}
                  onChange={setField("firstName")}
                  fullWidth
                  required
                  slotProps={{
                    input: {
                      startAdornment: (
                        <InputAdornment position="start">
                          <BadgeOutlinedIcon fontSize="small" />
                        </InputAdornment>
                      ),
                    },
                  }}
                />
                <TextField
                  label="Last name"
                  value={form.lastName}
                  onChange={setField("lastName")}
                  fullWidth
                  required
                  slotProps={{
                    input: {
                      startAdornment: (
                        <InputAdornment position="start">
                          <BadgeOutlinedIcon fontSize="small" />
                        </InputAdornment>
                      ),
                    },
                  }}
                />
              </Stack>

              <TextField
                label="Email"
                type="email"
                value={form.email}
                onChange={setField("email")}
                fullWidth
                required
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <EmailOutlinedIcon fontSize="small" />
                      </InputAdornment>
                    ),
                  },
                }}
              />
              <TextField
                label="Password"
                type="password"
                value={form.password}
                onChange={setField("password")}
                fullWidth
                required
                helperText="Minimum 8 characters"
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <LockOutlinedIcon fontSize="small" />
                      </InputAdornment>
                    ),
                  },
                }}
              />

              <Button
                type="submit"
                size="large"
                variant="contained"
                disabled={submitting}
                startIcon={!submitting && <PersonAddAltOutlinedIcon />}
                sx={{ py: 1.35, fontWeight: 800 }}
              >
                {submitting ? (
                  <CircularProgress size={24} color="inherit" />
                ) : (
                  "Create account"
                )}
              </Button>

              <Button
                variant="text"
                onClick={() => navigate("/login")}
                sx={{ fontWeight: 800 }}
              >
                Already have an account? Login
              </Button>
            </Stack>
          </Box>
        </Paper>
      </Box>

      <Box
        sx={{
          display: { xs: "none", md: "flex" },
          flexDirection: "column",
          justifyContent: "space-between",
          p: 6,
          color: "white",
          background: `linear-gradient(135deg, rgba(15,118,110,0.94), rgba(37,99,235,0.94)), url(${heroImage}) center/cover`,
        }}
      >
        <Stack direction="row" spacing={1.5} alignItems="center">
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2,
              display: "grid",
              placeItems: "center",
              bgcolor: "rgba(255,255,255,0.16)",
              border: "1px solid rgba(255,255,255,0.24)",
            }}
          >
            <StorefrontOutlinedIcon />
          </Box>
          <Box>
            <Typography variant="h6" fontWeight={800}>
              Khoi Micro
            </Typography>
            <Typography variant="body2" sx={{ opacity: 0.82 }}>
              E-commerce operations
            </Typography>
          </Box>
        </Stack>

        <Box sx={{ maxWidth: 560 }}>
          <Typography variant="h2" fontWeight={900} lineHeight={1.05}>
            Start managing the catalog with a profile-ready account.
          </Typography>
          <Typography variant="h6" sx={{ mt: 3, opacity: 0.84, lineHeight: 1.6 }}>
            Registration creates the identity user and emits the profile event
            for the rest of the microservice flow.
          </Typography>
        </Box>
      </Box>

      <Snackbar
        open={Boolean(errorMessage || successMessage)}
        autoHideDuration={5000}
        onClose={() => {
          setErrorMessage("");
          setSuccessMessage("");
        }}
        anchorOrigin={{ vertical: "top", horizontal: "right" }}
      >
        <Alert
          severity={errorMessage ? "error" : "success"}
          variant="filled"
          onClose={() => {
            setErrorMessage("");
            setSuccessMessage("");
          }}
        >
          {errorMessage || successMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
}
