import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AddShoppingCartOutlinedIcon from "@mui/icons-material/AddShoppingCartOutlined";
import AppsOutlinedIcon from "@mui/icons-material/AppsOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import { searchProductSuggestions } from "../services/productService";
import { isAuthenticated, logout } from "../services/authenticationService";
import { cartQuantity, getMyCart } from "../services/cartService";
import "./ShopStoreHeader.css";

const image = (product) => product?.thumbnailUrl || product?.images?.find((item) => item.isPrimary)?.url || product?.images?.[0]?.url;
const slugify = (value) => (value || "san-pham").toLocaleLowerCase("vi-VN").normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const url = (product) => `/shop/products/${slugify(product.name)}-${product.productId || product.id}`;
export default function ShopStoreHeader({ showBack = false }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [cartCount, setCartCount] = useState(0);
  const loggedIn = isAuthenticated();

  useEffect(() => {
    if (!loggedIn) return;
    const refreshCart = () => getMyCart().then((cart) => setCartCount(cartQuantity(cart))).catch(() => setCartCount(0));
    refreshCart();
    window.addEventListener("focus", refreshCart);
    window.addEventListener("nova:cart-changed", refreshCart);
    return () => { window.removeEventListener("focus", refreshCart); window.removeEventListener("nova:cart-changed", refreshCart); };
  }, [loggedIn]);
  useEffect(() => { const timer = window.setTimeout(() => searchProductSuggestions(query).then(setSuggestions).catch(() => setSuggestions([])), 300); return () => window.clearTimeout(timer); }, [query]);
  const leave = async () => { await logout(); window.location.assign("/shop"); };

  return <header className="store-header">
    <div className="store-header-top">
      <Link className="store-header-brand" to="/shop"><span>N</span>NovaShop</Link>
      <div className="store-header-search-wrap"><label className="store-header-search"><SearchOutlinedIcon /><input value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") navigate(`/shop/search?q=${encodeURIComponent(query)}`); }} placeholder="Tìm sản phẩm, thương hiệu..." aria-label="Tìm sản phẩm" /></label>{suggestions.length > 0 && <div className="store-header-results">{suggestions.map((product) => <Link key={product.productId || product.id} to={url(product)} onClick={() => setQuery("")}><img src={image(product)} alt="" /><span>{product.name}</span></Link>)}</div>}</div>
      <div className="store-header-actions"><Link aria-label="Sản phẩm yêu thích" to="/shop/wishlist"><FavoriteBorderOutlinedIcon /></Link><button type="button" aria-label="Thông báo"><NotificationsNoneOutlinedIcon /><b>3</b></button>{loggedIn ? <button className="store-header-logout" onClick={leave}>Đăng xuất</button> : <Link className="store-header-login" to="/shop/login">Đăng nhập</Link>}</div>
    </div>
    <nav className="store-header-nav" aria-label="Điều hướng cửa hàng">
      <Link to="/shop"><HomeOutlinedIcon />Trang chủ</Link>
      <Link to="/shop"><CategoryOutlinedIcon />Danh mục</Link>
      <Link to="/shop/hot-deals"><LocalOfferOutlinedIcon />Ưu đãi</Link>
      <Link to="/shop"><AppsOutlinedIcon />Hàng mới</Link>
      <Link to="/shop/orders"><ShoppingBagOutlinedIcon />Đơn hàng</Link>
      <Link to="/shop/wishlist"><FavoriteBorderOutlinedIcon />Yêu thích</Link>
      <Link to="/shop/account"><PersonOutlineOutlinedIcon />Tài khoản</Link>
      <Link className="store-header-cart" to="/cart"><AddShoppingCartOutlinedIcon />{cartCount > 0 && <b>{cartCount}</b>}</Link>
      {showBack && <button className="store-header-back" type="button" onClick={() => navigate(-1)}><ArrowBackOutlinedIcon />Quay lại</button>}
    </nav>
  </header>;
}
