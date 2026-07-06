import {
  Box,
  Divider,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Toolbar,
  Typography,
} from "@mui/material";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import LogoutOutlinedIcon from "@mui/icons-material/LogoutOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import { useLocation, useNavigate } from "react-router-dom";
import { logout } from "../services/authenticationService";

const menuItems = [
  {
    label: "Products",
    path: "/products",
    icon: <Inventory2OutlinedIcon />,
  },
  {
    label: "Categories",
    path: "/categories",
    icon: <CategoryOutlinedIcon />,
  },
  {
    label: "Search",
    path: "/search",
    icon: <SearchOutlinedIcon />,
  },
  {
    label: "Profile",
    path: "/profile",
    icon: <PersonOutlineOutlinedIcon />,
  },
  {
    label: "Notifications",
    path: "/notifications",
    icon: <NotificationsOutlinedIcon />,
  },
];

export default function SideMenu({ drawerWidth, mobileOpen, onClose }) {
  const navigate = useNavigate();
  const location = useLocation();

  const handleNavigate = (path) => {
    navigate(path);
    onClose?.();
  };

  const handleLogout = () => {
    logout();
    onClose?.();
    navigate("/login", { replace: true });
  };

  const drawerContent = (
    <Box sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <Toolbar>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <Box
            sx={{
              width: 38,
              height: 38,
              borderRadius: 2,
              display: "grid",
              placeItems: "center",
              bgcolor: "primary.main",
              color: "primary.contrastText",
            }}
          >
            <StorefrontOutlinedIcon fontSize="small" />
          </Box>
          <Box>
            <Typography fontWeight={900}>Khoi Micro</Typography>
            <Typography variant="caption" color="text.secondary">
              Admin workspace
            </Typography>
          </Box>
        </Stack>
      </Toolbar>
      <Divider />

      <List sx={{ px: 1.25, py: 2, flexGrow: 1 }}>
        {menuItems.map((item) => {
          const selected = location.pathname === item.path;

          return (
            <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                selected={selected}
                onClick={() => handleNavigate(item.path)}
                sx={{
                  borderRadius: 2,
                  minHeight: 46,
                  "&.Mui-selected": {
                    bgcolor: "primary.main",
                    color: "primary.contrastText",
                    "&:hover": {
                      bgcolor: "primary.dark",
                    },
                    "& .MuiListItemIcon-root": {
                      color: "primary.contrastText",
                    },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 40 }}>{item.icon}</ListItemIcon>
                <ListItemText
                  primary={item.label}
                  primaryTypographyProps={{ fontWeight: 800 }}
                />
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>

      <Box sx={{ p: 1.25 }}>
        <Divider sx={{ mb: 1.25 }} />
        <ListItem disablePadding>
          <ListItemButton onClick={handleLogout} sx={{ borderRadius: 2 }}>
            <ListItemIcon sx={{ minWidth: 40 }}>
              <LogoutOutlinedIcon />
            </ListItemIcon>
            <ListItemText
              primary="Logout"
              primaryTypographyProps={{ fontWeight: 800 }}
            />
          </ListItemButton>
        </ListItem>
      </Box>
    </Box>
  );

  return (
    <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: 0 }}>
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onClose}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: "block", md: "none" },
          "& .MuiDrawer-paper": {
            width: drawerWidth,
          },
        }}
      >
        {drawerContent}
      </Drawer>
      <Drawer
        variant="permanent"
        open
        sx={{
          display: { xs: "none", md: "block" },
          "& .MuiDrawer-paper": {
            width: drawerWidth,
            borderRight: "1px solid",
            borderColor: "divider",
          },
        }}
      >
        {drawerContent}
      </Drawer>
    </Box>
  );
}
