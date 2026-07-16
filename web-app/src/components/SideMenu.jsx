import {
  Badge,
  Box,
  Chip,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import AnalyticsOutlinedIcon from "@mui/icons-material/AnalyticsOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import ChevronLeftRoundedIcon from "@mui/icons-material/ChevronLeftRounded";
import DashboardOutlinedIcon from "@mui/icons-material/DashboardOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import LocalFireDepartmentOutlinedIcon from "@mui/icons-material/LocalFireDepartmentOutlined";
import LogoutOutlinedIcon from "@mui/icons-material/LogoutOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import PaymentsOutlinedIcon from "@mui/icons-material/PaymentsOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StarBorderRoundedIcon from "@mui/icons-material/StarBorderRounded";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import { useLocation, useNavigate } from "react-router-dom";
import { logout } from "../services/authenticationService";

const menuGroups = [
  { label: "Workspace", items: [{ label: "Dashboard", path: "/dashboard", icon: <DashboardOutlinedIcon /> }, { label: "Analytics", path: "/orders", icon: <AnalyticsOutlinedIcon />, badge: "Live" }] },
  { label: "Store service", items: [{ label: "Products", path: "/products", icon: <Inventory2OutlinedIcon /> }, { label: "Categories", path: "/categories", icon: <CategoryOutlinedIcon />, badge: "" }, { label: "Search", path: "/search", icon: <SearchOutlinedIcon /> }, { label: "Orders", path: "/orders", icon: <ShoppingBagOutlinedIcon /> }, { label: "Payments", path: "/payments", icon: <PaymentsOutlinedIcon /> }] },
  { label: "Account", items: [{ label: "Notifications", path: "/notifications", icon: <NotificationsNoneOutlinedIcon /> }, { label: "Profile", path: "/profile", icon: <PersonOutlineOutlinedIcon /> }, { label: "Settings", path: "/profile", icon: <SettingsOutlinedIcon /> }] },
];

const workflowRuns = [
  { label: "Product catalog sync", color: "#2563eb" },
  { label: "Order status pipeline", color: "#0f766e" },
];

export default function SideMenu({ drawerWidth, collapsed, mobileOpen, onClose, onToggle }) {
  const navigate = useNavigate();
  const location = useLocation();
  const handleLogout = () => { logout(); onClose?.(); navigate("/login", { replace: true }); };
  const handleNavigate = (path) => { navigate(path); onClose?.(); };

  const drawerContent = (
    <Box className={`sidebar-content ${collapsed ? "sidebar-collapsed" : ""}`}>
      <Stack className="brand-row" direction="row" alignItems="center" justifyContent="space-between">
        <Stack direction="row" spacing={1.2} alignItems="center" className="brand-lockup">
          <Box className="brand-mark"><StorefrontOutlinedIcon fontSize="small" /></Box>
          {!collapsed && <Box><Typography className="brand-name">Khoi<span>Commerce</span></Typography><Typography className="brand-caption">Admin workspace</Typography></Box>}
        </Stack>
        <Tooltip title={collapsed ? "Expand sidebar" : "Collapse sidebar"}><IconButton className="sidebar-toggle" onClick={onToggle} aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}><ChevronLeftRoundedIcon sx={{ transform: collapsed ? "rotate(180deg)" : "none" }} /></IconButton></Tooltip>
      </Stack>

      <Box className="sidebar-scroll">
        {menuGroups.map((group) => <Box key={group.label} className="menu-group"><Typography className="menu-label">{!collapsed && group.label}</Typography><List disablePadding>{group.items.map((item) => { const selected = location.pathname === item.path || (location.pathname === "/" && item.path === "/dashboard"); return <ListItem key={`${group.label}-${item.label}`} disablePadding><Tooltip title={collapsed ? item.label : ""} placement="right"><ListItemButton className="menu-item" selected={selected} onClick={() => handleNavigate(item.path)}><ListItemIcon>{item.icon}</ListItemIcon>{!collapsed && <ListItemText primary={item.label} />}{!collapsed && item.badge === "Live" && <Chip label="Live" size="small" className="menu-badge" />}{!collapsed && item.label === "Search" && <LocalFireDepartmentOutlinedIcon className="hot-icon" />}</ListItemButton></Tooltip></ListItem>; })}</List></Box>)}

        {!collapsed && <Box className="workflow-block"><Stack direction="row" justifyContent="space-between" alignItems="center"><Typography className="menu-label">Recent activity</Typography><IconButton size="small" aria-label="Favorite activity"><StarBorderRoundedIcon fontSize="small" /></IconButton></Stack><Stack spacing={0.5}>{workflowRuns.map((run) => <Stack direction="row" spacing={1} alignItems="center" className="workflow-item" key={run.label}><Box className="workflow-dot" sx={{ bgcolor: run.color }} /><Typography noWrap>{run.label}</Typography><StarBorderRoundedIcon className="workflow-star" fontSize="inherit" /></Stack>)}</Stack></Box>}
      </Box>

      {!collapsed && <Box className="upgrade-card"><Stack direction="row" justifyContent="space-between" alignItems="flex-start"><Box className="upgrade-icon"><LocalFireDepartmentOutlinedIcon /></Box><Typography className="upgrade-percent">72%</Typography></Stack><Typography className="upgrade-title">Workspace health</Typography><Typography className="upgrade-copy">Your store services are ready for the next release.</Typography><Box className="upgrade-progress"><Box sx={{ width: "72%" }} /></Box></Box>}

      <Divider className="sidebar-divider" />
      <Tooltip title={collapsed ? "Logout" : ""} placement="right"><ListItem disablePadding><ListItemButton className="logout-item" onClick={handleLogout}><ListItemIcon><LogoutOutlinedIcon /></ListItemIcon>{!collapsed && <ListItemText primary="Logout" />}</ListItemButton></ListItem></Tooltip>
    </Box>
  );

  return <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: 0 }}><Drawer variant="temporary" open={mobileOpen} onClose={onClose} ModalProps={{ keepMounted: true }} sx={{ display: { xs: "block", md: "none" }, "& .MuiDrawer-paper": { width: 270 } }}>{drawerContent}</Drawer><Drawer variant="permanent" open sx={{ display: { xs: "none", md: "block" }, "& .MuiDrawer-paper": { width: drawerWidth, boxSizing: "border-box", border: 0, bgcolor: "transparent", overflow: "visible" } }}>{drawerContent}</Drawer></Box>;
}
