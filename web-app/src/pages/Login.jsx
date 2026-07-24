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
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import SecurityOutlinedIcon from "@mui/icons-material/SecurityOutlined";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getRoleHomePath, isAuthenticated, login } from "../services/authenticationService";
import heroImage from "../assets/hero.png";

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("admin@khoimicro.com");
  const [password, setPassword] = useState("12345678");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (isAuthenticated()) {
      navigate(getRoleHomePath(), { replace: true });
    }
  }, [navigate]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");

    try {
      await login(email, password);
      navigate(getRoleHomePath(), { replace: true });
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message ||
          "Login failed. Please check your email and password.",
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
        gridTemplateColumns: { xs: "1fr", md: "1.05fr 0.95fr" },
        bgcolor: "#f6f8fc",
      }}
    >
      <Box
        sx={{
          display: { xs: "none", md: "flex" },
          flexDirection: "column",
          justifyContent: "space-between",
          p: 6,
          color: "white",
          background: `linear-gradient(135deg, rgba(37,99,235,0.96), rgba(15,118,110,0.92)), url(${heroImage}) center/cover`,
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
            Manage products, categories, and sellers in one clean workspace.
          </Typography>
          <Typography variant="h6" sx={{ mt: 3, opacity: 0.84, lineHeight: 1.6 }}>
            Sign in to control catalog data, review profile information, and
            keep notifications close to the work.
          </Typography>
        </Box>

        <Stack direction="row" spacing={2}>
          {[
            ["Catalog", <Inventory2OutlinedIcon key="catalog" />],
            ["Secure", <SecurityOutlinedIcon key="secure" />],
            ["Gateway", <LocalShippingOutlinedIcon key="gateway" />],
          ].map(([label, icon]) => (
            <Stack
              key={label}
              direction="row"
              spacing={1}
              alignItems="center"
              sx={{
                px: 1.5,
                py: 1,
                borderRadius: 2,
                bgcolor: "rgba(255,255,255,0.14)",
                border: "1px solid rgba(255,255,255,0.2)",
              }}
            >
              {icon}
              <Typography fontWeight={700}>{label}</Typography>
            </Stack>
          ))}
        </Stack>
      </Box>

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
            maxWidth: 440,
            p: { xs: 3, sm: 4 },
            border: "1px solid",
            borderColor: "divider",
            boxShadow: "0 24px 70px rgba(15, 23, 42, 0.10)",
          }}
        >
          <Stack spacing={0.75} sx={{ mb: 3 }}>
            <Typography variant="overline" color="primary" fontWeight={800}>
              Welcome back
            </Typography>
            <Typography variant="h4" fontWeight={900}>
              Sign in
            </Typography>
            <Typography color="text.secondary">
              Use your admin or seller account to continue.
            </Typography>
          </Stack>

          <Box component="form" onSubmit={handleSubmit}>
            <Stack spacing={2.25}>
              <TextField
                label="Email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
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
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                fullWidth
                required
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
                sx={{ py: 1.35, fontWeight: 800 }}
              >
                {submitting ? (
                  <CircularProgress size={24} color="inherit" />
                ) : (
                  "Login"
                )}
              </Button>
              <Button
                variant="text"
                onClick={() => navigate("/register")}
                sx={{ fontWeight: 800 }}
              >
                Create a new account
              </Button>
            </Stack>
          </Box>
        </Paper>
      </Box>

      <Snackbar
        open={Boolean(errorMessage)}
        autoHideDuration={5000}
        onClose={() => setErrorMessage("")}
        anchorOrigin={{ vertical: "top", horizontal: "right" }}
      >
        <Alert
          severity="error"
          variant="filled"
          onClose={() => setErrorMessage("")}
        >
          {errorMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
}
