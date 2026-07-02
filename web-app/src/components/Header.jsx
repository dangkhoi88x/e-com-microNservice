import {
  AppBar,
  Avatar,
  Badge,
  Box,
  IconButton,
  Menu,
  MenuItem,
  Stack,
  Toolbar,
  Tooltip,
  Typography,
} from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import AccountCircleOutlinedIcon from "@mui/icons-material/AccountCircleOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import LogoutOutlinedIcon from "@mui/icons-material/LogoutOutlined";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { logout } from "../services/authenticationService";

export default function Header({ drawerWidth, onMenuClick, showMenuButton }) {
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);
  const menuOpen = Boolean(anchorEl);

  const handleLogout = () => {
    setAnchorEl(null);
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        width: { md: `calc(100% - ${drawerWidth}px)` },
        ml: { md: `${drawerWidth}px` },
        bgcolor: "background.paper",
        color: "text.primary",
        borderBottom: "1px solid",
        borderColor: "divider",
      }}
    >
      <Toolbar sx={{ gap: 1.5 }}>
        {showMenuButton && (
          <IconButton edge="start" onClick={onMenuClick} aria-label="open menu">
            <MenuIcon />
          </IconButton>
        )}

        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h6" fontWeight={900}>
            Dashboard
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Product and catalog management
          </Typography>
        </Box>

        <Tooltip title="Notifications">
          <IconButton onClick={() => navigate("/notifications")}>
            <Badge color="error" variant="dot">
              <NotificationsOutlinedIcon />
            </Badge>
          </IconButton>
        </Tooltip>

        <Tooltip title="Account">
          <IconButton onClick={(event) => setAnchorEl(event.currentTarget)}>
            <Avatar sx={{ width: 34, height: 34, bgcolor: "primary.main" }}>
              <AccountCircleOutlinedIcon fontSize="small" />
            </Avatar>
          </IconButton>
        </Tooltip>

        <Menu
          anchorEl={anchorEl}
          open={menuOpen}
          onClose={() => setAnchorEl(null)}
          anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
          transformOrigin={{ vertical: "top", horizontal: "right" }}
        >
          <MenuItem onClick={() => navigate("/profile")}>
            <Stack direction="row" spacing={1.25} alignItems="center">
              <StorefrontOutlinedIcon fontSize="small" />
              <span>Profile</span>
            </Stack>
          </MenuItem>
          <MenuItem onClick={handleLogout}>
            <Stack direction="row" spacing={1.25} alignItems="center">
              <LogoutOutlinedIcon fontSize="small" />
              <span>Logout</span>
            </Stack>
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
}
