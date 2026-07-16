import { Box, Toolbar, useMediaQuery, useTheme } from "@mui/material";
import { useState } from "react";
import Header from "../components/Header";
import SideMenu from "../components/SideMenu";

const expandedWidth = 252;
const collapsedWidth = 86;

export default function MainLayout({ children }) {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up("md"));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const drawerWidth = collapsed ? collapsedWidth : expandedWidth;

  return <Box className="app-shell"><Header drawerWidth={drawerWidth} onMenuClick={() => setMobileOpen((open) => !open)} showMenuButton={!isDesktop} /><SideMenu drawerWidth={drawerWidth} collapsed={collapsed && isDesktop} mobileOpen={mobileOpen} onClose={() => setMobileOpen(false)} onToggle={() => setCollapsed((value) => !value)} /><Box component="main" className="main-content" sx={{ ml: { md: `${drawerWidth}px` } }}><Toolbar /><Box className="admin-page-root">{children}</Box></Box></Box>;
}
