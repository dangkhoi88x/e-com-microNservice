import { Box, Toolbar, useMediaQuery, useTheme } from "@mui/material";
import { useState } from "react";
import Header from "../components/Header";
import SideMenu from "../components/SideMenu";

const drawerWidth = 280;

export default function MainLayout({ children }) {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up("md"));
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleDrawerToggle = () => {
    setMobileOpen((open) => !open);
  };

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <Header
        drawerWidth={drawerWidth}
        onMenuClick={handleDrawerToggle}
        showMenuButton={!isDesktop}
      />
      <SideMenu
        drawerWidth={drawerWidth}
        mobileOpen={mobileOpen}
        onClose={() => setMobileOpen(false)}
      />
      <Box
        component="main"
        sx={{
          ml: { md: `${drawerWidth}px` },
          px: { xs: 2, sm: 3 },
          py: 3,
        }}
      >
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
}
