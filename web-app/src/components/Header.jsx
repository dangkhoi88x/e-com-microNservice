import { AppBar, Avatar, Badge, Box, IconButton, InputBase, Menu, MenuItem, Toolbar, Tooltip } from "@mui/material";
import AccountCircleOutlinedIcon from "@mui/icons-material/AccountCircleOutlined";
import MenuIcon from "@mui/icons-material/Menu";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import SearchRoundedIcon from "@mui/icons-material/SearchRounded";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { hasAnyRole, logout } from "../services/authenticationService";

export default function Header({ drawerWidth, onMenuClick, showMenuButton }) {
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);
  const canRegisterSeller = !hasAnyRole("ROLE_SELLER", "SELLER", "ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN");
  const handleLogout = async () => { setAnchorEl(null); await logout(); navigate("/login", { replace: true }); };

  return <AppBar position="fixed" elevation={0} className="topbar" sx={{ width: { md: `calc(100% - ${drawerWidth}px)` }, ml: { md: `${drawerWidth}px` } }}><Toolbar className="topbar-toolbar">{showMenuButton && <IconButton onClick={onMenuClick} className="mobile-menu-button" aria-label="Open menu"><MenuIcon /></IconButton>}<Box className="breadcrumb"><span>Workspace</span><span className="breadcrumb-separator">/</span><strong>Dashboard</strong></Box><Box className="topbar-spacer" /><Box className="global-search"><SearchRoundedIcon fontSize="small" /><InputBase placeholder="Search products, orders..." inputProps={{ "aria-label": "Search products and orders" }} /></Box><Tooltip title="Notifications"><IconButton className="topbar-icon" onClick={() => navigate("/notifications")} aria-label="Open notifications"><Badge color="error" variant="dot"><NotificationsNoneOutlinedIcon /></Badge></IconButton></Tooltip><Tooltip title="Settings"><IconButton className="topbar-icon" onClick={() => navigate("/profile")} aria-label="Open settings"><SettingsOutlinedIcon /></IconButton></Tooltip><Tooltip title="Account"><IconButton className="avatar-button" onClick={(event) => setAnchorEl(event.currentTarget)} aria-label="Open account menu"><Avatar><AccountCircleOutlinedIcon fontSize="small" /></Avatar></IconButton></Tooltip><Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)} anchorOrigin={{ vertical: "bottom", horizontal: "right" }} transformOrigin={{ vertical: "top", horizontal: "right" }}><MenuItem onClick={() => navigate("/profile")}>Profile</MenuItem>{canRegisterSeller && <MenuItem onClick={() => { setAnchorEl(null); navigate("/seller/register"); }}>Register as seller</MenuItem>}<MenuItem onClick={handleLogout}>Logout</MenuItem></Menu></Toolbar></AppBar>;
}
