import { AppBar, Avatar, Badge, Box, ClickAwayListener, IconButton, InputBase, Menu, MenuItem, Toolbar, Tooltip, Typography } from "@mui/material";
import AccountCircleOutlinedIcon from "@mui/icons-material/AccountCircleOutlined";
import MenuIcon from "@mui/icons-material/Menu";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import SearchRoundedIcon from "@mui/icons-material/SearchRounded";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { hasAnyRole, logout } from "../services/authenticationService";
import { searchProductSuggestions } from "../services/productService";

const looksLikeOrderCode = (value) => /^(ORD[-\s]|[a-f0-9]{8}-[a-f0-9-]{27}$)/i.test(value.trim());

export default function Header({ drawerWidth, onMenuClick, showMenuButton }) {
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [searchOpen, setSearchOpen] = useState(false);
  const canRegisterSeller = !hasAnyRole("ROLE_SELLER", "SELLER", "ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN");
  const handleLogout = async () => { setAnchorEl(null); await logout(); navigate("/login", { replace: true }); };

  useEffect(() => {
    const value = query.trim();
    if (value.length < 2 || looksLikeOrderCode(value)) { setSuggestions([]); return undefined; }
    const timer = window.setTimeout(() => {
      searchProductSuggestions(value, 5).then(setSuggestions).catch(() => setSuggestions([]));
    }, 250);
    return () => window.clearTimeout(timer);
  }, [query]);

  const submitSearch = (value = query) => {
    const normalized = value.trim();
    if (!normalized) return;
    setSearchOpen(false);
    setSuggestions([]);
    navigate(looksLikeOrderCode(normalized)
      ? `/admin/orders?query=${encodeURIComponent(normalized)}`
      : `/admin/search?name=${encodeURIComponent(normalized)}`);
  };

  return <AppBar position="fixed" elevation={0} className="topbar" sx={{ width: { md: `calc(100% - ${drawerWidth}px)` }, ml: { md: `${drawerWidth}px` } }}><Toolbar className="topbar-toolbar">{showMenuButton && <IconButton onClick={onMenuClick} className="mobile-menu-button" aria-label="Open menu"><MenuIcon /></IconButton>}<Box className="breadcrumb"><span>Workspace</span><span className="breadcrumb-separator">/</span><strong>Dashboard</strong></Box><Box className="topbar-spacer" /><ClickAwayListener onClickAway={() => setSearchOpen(false)}><Box className="global-search-wrap"><Box className="global-search"><SearchRoundedIcon fontSize="small" /><InputBase value={query} onFocus={() => setSearchOpen(true)} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") submitSearch(); }} placeholder="Tìm sản phẩm, mã đơn..." inputProps={{ "aria-label": "Tìm sản phẩm hoặc mã đơn" }} /></Box>{searchOpen && (suggestions.length > 0 || query.trim().length >= 2) && <Box className="global-search-results">{suggestions.length > 0 ? suggestions.map((product) => <Box className="global-search-result" key={product.productId || product.id} onClick={() => submitSearch(product.name)}><Box className="global-search-result-mark">{(product.name || "?").slice(0, 1)}</Box><Box sx={{ minWidth: 0 }}><Typography noWrap fontWeight={700} fontSize={12}>{product.name}</Typography><Typography noWrap color="text.secondary" fontSize={10.5}>{product.categoryName || "Sản phẩm"}</Typography></Box></Box>) : <Box className="global-search-empty" onClick={() => submitSearch()}>Nhấn Enter để tìm “{query.trim()}”</Box>}</Box>}</Box></ClickAwayListener><Tooltip title="Notifications"><IconButton className="topbar-icon" onClick={() => navigate("/notifications")} aria-label="Open notifications"><Badge color="error" variant="dot"><NotificationsNoneOutlinedIcon /></Badge></IconButton></Tooltip><Tooltip title="Settings"><IconButton className="topbar-icon" onClick={() => navigate("/profile")} aria-label="Open settings"><SettingsOutlinedIcon /></IconButton></Tooltip><Tooltip title="Account"><IconButton className="avatar-button" onClick={(event) => setAnchorEl(event.currentTarget)} aria-label="Open account menu"><Avatar><AccountCircleOutlinedIcon fontSize="small" /></Avatar></IconButton></Tooltip><Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)} anchorOrigin={{ vertical: "bottom", horizontal: "right" }} transformOrigin={{ vertical: "top", horizontal: "right" }}><MenuItem onClick={() => navigate("/profile")}>Profile</MenuItem>{canRegisterSeller && <MenuItem onClick={() => { setAnchorEl(null); navigate("/seller/register"); }}>Register as seller</MenuItem>}<MenuItem onClick={handleLogout}>Logout</MenuItem></Menu></Toolbar></AppBar>;
}
