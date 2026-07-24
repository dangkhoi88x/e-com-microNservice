import {
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
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import LogoutOutlinedIcon from "@mui/icons-material/LogoutOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import PaymentsOutlinedIcon from "@mui/icons-material/PaymentsOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import StoreOutlinedIcon from "@mui/icons-material/StoreOutlined";
import { useLocation, useNavigate } from "react-router-dom";
import { hasAnyRole, logout } from "../services/authenticationService";

const adminMenuGroups = [
  { label: "Workspace", items: [{ label: "Dashboard", path: "/admin", icon: <DashboardOutlinedIcon /> }, { label: "Analytics", path: "/admin/orders", icon: <AnalyticsOutlinedIcon />, badge: "Live" }] },
  { label: "Store service", items: [{ label: "Products", path: "/admin/products", icon: <Inventory2OutlinedIcon /> }, { label: "Seller approvals", path: "/admin/sellers", icon: <StoreOutlinedIcon /> }, { label: "Categories", path: "/admin/categories", icon: <CategoryOutlinedIcon />, badge: "" }, { label: "Promotions", path: "/admin/promotions", icon: <LocalOfferOutlinedIcon /> }, { label: "Flash Sale", path: "/admin/flash-deals", icon: <LocalFireDepartmentOutlinedIcon /> }, { label: "Search", path: "/admin/search", icon: <SearchOutlinedIcon /> }, { label: "Orders", path: "/admin/orders", icon: <ShoppingBagOutlinedIcon /> }, { label: "Shipments", path: "/admin/shipments", icon: <LocalShippingOutlinedIcon /> }, { label: "Payments", path: "/admin/payments", icon: <PaymentsOutlinedIcon /> }] },
  { label: "Account", items: [{ label: "Notifications", path: "/notifications", icon: <NotificationsNoneOutlinedIcon /> }, { label: "Profile", path: "/profile", icon: <PersonOutlineOutlinedIcon /> }, { label: "Settings", path: "/profile", icon: <SettingsOutlinedIcon /> }] },
];

const sellerMenuGroups = [
  { label: "Seller center", items: [{ label: "Tổng quan", path: "/seller", icon: <DashboardOutlinedIcon /> }, { label: "Sản phẩm", path: "/seller/products", icon: <Inventory2OutlinedIcon /> }, { label: "Đơn hàng", path: "/seller/orders", icon: <ShoppingBagOutlinedIcon /> }, { label: "Khuyến mãi", path: "/seller/promotions", icon: <LocalFireDepartmentOutlinedIcon /> }, { label: "Thông tin shop", path: "/seller/shop", icon: <StoreOutlinedIcon /> }] },
  { label: "Account", items: [{ label: "Notifications", path: "/notifications", icon: <NotificationsNoneOutlinedIcon /> }, { label: "Profile", path: "/profile", icon: <PersonOutlineOutlinedIcon /> }] },
];

export default function SideMenu({ drawerWidth, collapsed, mobileOpen, onClose, onToggle }) {
  const navigate = useNavigate();
  const location = useLocation();
  const isAdmin = hasAnyRole("ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN");
  const isSeller = hasAnyRole("ROLE_SELLER", "SELLER");
  const menuGroups = isAdmin ? adminMenuGroups : isSeller ? sellerMenuGroups : [];
  const workspaceLabel = isAdmin ? "Admin workspace" : isSeller ? "Seller center" : "Workspace";
  const handleLogout = async () => { await logout(); onClose?.(); navigate("/login", { replace: true }); };
  const handleNavigate = (path) => { navigate(path); onClose?.(); };

  const drawerContent = (
    <Box className={`sidebar-content ${collapsed ? "sidebar-collapsed" : ""}`}>
      <Stack className="brand-row" direction="row" alignItems="center" justifyContent="space-between">
        <Stack direction="row" spacing={1.2} alignItems="center" className="brand-lockup">
          <Box className="brand-mark"><StorefrontOutlinedIcon fontSize="small" /></Box>
          {!collapsed && <Box><Typography className="brand-name">Khoi<span>Commerce</span></Typography><Typography className="brand-caption">{workspaceLabel}</Typography></Box>}
        </Stack>
        <Tooltip title={collapsed ? "Expand sidebar" : "Collapse sidebar"}><IconButton className="sidebar-toggle" onClick={onToggle} aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}><ChevronLeftRoundedIcon sx={{ transform: collapsed ? "rotate(180deg)" : "none" }} /></IconButton></Tooltip>
      </Stack>

      <Box className="sidebar-scroll">
        {menuGroups.map((group) => <Box key={group.label} className="menu-group"><Typography className="menu-label">{!collapsed && group.label}</Typography><List disablePadding>{group.items.map((item) => { const selected = location.pathname === item.path; return <ListItem key={`${group.label}-${item.label}`} disablePadding><Tooltip title={collapsed ? item.label : ""} placement="right"><ListItemButton className="menu-item" selected={selected} onClick={() => handleNavigate(item.path)}><ListItemIcon>{item.icon}</ListItemIcon>{!collapsed && <ListItemText primary={item.label} />}{!collapsed && item.badge === "Live" && <Chip label="Live" size="small" className="menu-badge" />}{!collapsed && item.label === "Search" && <LocalFireDepartmentOutlinedIcon className="hot-icon" />}</ListItemButton></Tooltip></ListItem>; })}</List></Box>)}
      </Box>

      {!collapsed && <Box className="upgrade-card"><Stack direction="row" justifyContent="space-between" alignItems="flex-start"><Box className="upgrade-icon"><LocalFireDepartmentOutlinedIcon /></Box><Typography className="upgrade-percent">72%</Typography></Stack><Typography className="upgrade-title">Workspace health</Typography><Typography className="upgrade-copy">Your store services are ready for the next release.</Typography><Box className="upgrade-progress"><Box sx={{ width: "72%" }} /></Box></Box>}

      <Divider className="sidebar-divider" />
      <Tooltip title={collapsed ? "Logout" : ""} placement="right"><ListItem disablePadding><ListItemButton className="logout-item" onClick={handleLogout}><ListItemIcon><LogoutOutlinedIcon /></ListItemIcon>{!collapsed && <ListItemText primary="Logout" />}</ListItemButton></ListItem></Tooltip>
    </Box>
  );

  return <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: 0 }}><Drawer variant="temporary" open={mobileOpen} onClose={onClose} ModalProps={{ keepMounted: true }} sx={{ display: { xs: "block", md: "none" }, "& .MuiDrawer-paper": { width: 270 } }}>{drawerContent}</Drawer><Drawer variant="permanent" open sx={{ display: { xs: "none", md: "block" }, "& .MuiDrawer-paper": { width: drawerWidth, boxSizing: "border-box", border: 0, bgcolor: "transparent", overflow: "visible" } }}>{drawerContent}</Drawer></Box>;
}
