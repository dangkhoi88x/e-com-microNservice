import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AddShoppingCartOutlinedIcon from "@mui/icons-material/AddShoppingCartOutlined";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import RemoveOutlinedIcon from "@mui/icons-material/RemoveOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import { searchProductSuggestions } from "../services/productService";
import { isAuthenticated, logout } from "../services/authenticationService";
import { cartQuantity, getMyCart, removeCartItem, updateCartItem } from "../services/cartService";
import { getMyNotifications } from "../services/notificationService";
import "./ShopStoreHeader.css";
import "./ShopStoreNotifications.css";
import "./ShopStoreMiniCart.css";
import "./ShopStoreMiniCartActions.css";

const image = (product) => product?.thumbnailUrl || product?.images?.find((item) => item.isPrimary)?.url || product?.images?.[0]?.url;
const slugify = (value) => (value || "san-pham").toLocaleLowerCase("vi-VN").normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const url = (product) => `/shop/products/${slugify(product.name)}-${product.productId || product.id}`;
export default function ShopStoreHeader({ showBack = false }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [cartCount, setCartCount] = useState(0);
  const [cartOpen, setCartOpen] = useState(false);
  const [cartItems, setCartItems] = useState([]);
  const [cartLoading, setCartLoading] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [notificationError, setNotificationError] = useState("");
  const notificationRef = useRef(null);
  const cartRef = useRef(null);
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
  useEffect(() => { const close = (event) => { if (!notificationRef.current?.contains(event.target)) setNotificationOpen(false); if (!cartRef.current?.contains(event.target)) setCartOpen(false); }; document.addEventListener("mousedown", close); return () => document.removeEventListener("mousedown", close); }, []);
  const leave = async () => { await logout(); window.location.assign("/shop"); };
  const toggleNotifications = async () => {
    if (!loggedIn) return navigate("/shop/login?redirect=/shop");
    const nextOpen = !notificationOpen;
    setNotificationOpen(nextOpen);
    if (!nextOpen) return;
    setNotificationsLoading(true); setNotificationError("");
    try { setNotifications(await getMyNotifications()); } catch (error) { setNotificationError(error.response?.data?.message || "Không thể tải thông báo."); } finally { setNotificationsLoading(false); }
  };
  const toggleCart = async () => {
    if (!loggedIn) return navigate("/shop/login?redirect=/shop");
    const nextOpen = !cartOpen; setCartOpen(nextOpen);
    if (!nextOpen) return;
    setCartLoading(true);
    try { const cart = await getMyCart(); setCartItems(cart.items || []); setCartCount(cartQuantity(cart)); } finally { setCartLoading(false); }
  };
  const miniCartTotal = cartItems.reduce((total, item) => total + Number(item.price || 0) * Number(item.quantity || 0), 0);
  const changeMiniCartQuantity = async (item, delta) => {
    try {
      const cart = Number(item.quantity || 1) + delta <= 0 ? await removeCartItem(item.id) : await updateCartItem(item.id, { quantity: Number(item.quantity || 1) + delta, selected: item.selected });
      setCartItems(cart.items || []); setCartCount(cartQuantity(cart));
    } catch { /* preserve current mini-cart when Cart Service rejects the update */ }
  };
  const removeMiniCartItem = async (itemId) => {
    try { const cart = await removeCartItem(itemId); setCartItems(cart.items || []); setCartCount(cartQuantity(cart)); } catch { /* preserve current mini-cart */ }
  };

  return <header className="store-header">
    <div className="store-header-top">
      <Link className="store-header-brand" to="/shop"><span>N</span>NovaShop</Link>
      <div className="store-header-search-wrap"><label className="store-header-search"><SearchOutlinedIcon /><input value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") navigate(`/shop/categories?q=${encodeURIComponent(query)}`); }} placeholder="Tìm sản phẩm, thương hiệu..." aria-label="Tìm sản phẩm" /></label>{suggestions.length > 0 && <div className="store-header-results">{suggestions.map((product) => <Link key={product.productId || product.id} to={url(product)} onClick={() => setQuery("")}><img src={image(product)} alt="" /><span>{product.name}</span></Link>)}</div>}</div>
      <div className="store-header-actions"><Link aria-label="Sản phẩm yêu thích" to="/shop/wishlist"><FavoriteBorderOutlinedIcon /></Link><div className="store-header-notifications" ref={notificationRef}><button type="button" aria-label="Thông báo" onClick={toggleNotifications}><NotificationsNoneOutlinedIcon />{notifications.filter((item) => !item.read).length > 0 && <b>{Math.min(9, notifications.filter((item) => !item.read).length)}</b>}</button>{notificationOpen && <section className="store-notification-popover"><header><strong>Thông báo</strong><span>{notifications.length}</span></header>{notificationsLoading ? <p className="store-notification-state">Đang tải thông báo…</p> : notificationError ? <p className="store-notification-state is-error">{notificationError}</p> : notifications.length ? <div>{notifications.slice(0, 6).map((item) => <article key={item.id}><span className={item.type === "FLASH_SALE_UPCOMING" ? "flash" : ""}><NotificationsNoneOutlinedIcon /></span><p><b>{item.title}</b><small>{item.message}</small><time>{item.createdAt ? new Date(item.createdAt).toLocaleString("vi-VN") : ""}</time></p></article>)}</div> : <p className="store-notification-state">Chưa có thông báo mới.</p>}<Link to="/shop/account?tab=notifications" onClick={() => setNotificationOpen(false)}>Xem tất cả</Link></section>}</div>{loggedIn ? <button className="store-header-logout" onClick={leave}>Đăng xuất</button> : <Link className="store-header-login" to="/shop/login">Đăng nhập</Link>}</div>
    </div>
    <nav className="store-header-nav" aria-label="Điều hướng cửa hàng">
      <Link to="/shop"><HomeOutlinedIcon />Trang chủ</Link>
      <Link to="/shop/categories"><CategoryOutlinedIcon />Danh mục</Link>
      <Link to="/shop/hot-deals"><LocalOfferOutlinedIcon />Ưu đãi</Link>
      <Link to="/shop/orders"><ShoppingBagOutlinedIcon />Đơn hàng</Link>
      <Link to="/shop/wishlist"><FavoriteBorderOutlinedIcon />Yêu thích</Link>
      <Link to="/shop/account"><PersonOutlineOutlinedIcon />Tài khoản</Link>
      <div className="store-header-mini-cart" ref={cartRef}><button className="store-header-cart" type="button" onClick={toggleCart} aria-label="Mở giỏ hàng"><AddShoppingCartOutlinedIcon />{cartCount > 0 && <b>{cartCount}</b>}</button>{cartOpen && <section className="store-cart-popover"><header><strong>Giỏ hàng của bạn</strong><span>{cartCount} sản phẩm</span></header>{cartLoading ? <p>Đang tải giỏ hàng…</p> : cartItems.length ? <><div className="store-cart-popover-items">{cartItems.slice(0, 4).map((item) => <article key={item.id}><img src={item.imageUrl || "https://placehold.co/80x80/e7f2f8/3b82c4?text=Nova"} alt="" /><div><b>{item.productName}</b><small>{new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(item.price || 0))}</small><section className="store-mini-cart-quantity"><button type="button" onClick={() => changeMiniCartQuantity(item, -1)} aria-label="Giảm số lượng"><RemoveOutlinedIcon /></button><span>{item.quantity}</span><button type="button" onClick={() => changeMiniCartQuantity(item, 1)} aria-label="Tăng số lượng"><AddOutlinedIcon /></button></section></div><button type="button" className="store-mini-cart-remove" onClick={() => removeMiniCartItem(item.id)} aria-label="Xóa sản phẩm"><DeleteOutlineOutlinedIcon /></button></article>)}</div><footer><p><span>Tạm tính</span><b>{new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(miniCartTotal)}</b></p><Link to="/cart" onClick={() => setCartOpen(false)}>Xem tất cả giỏ hàng</Link></footer></> : <p>Giỏ hàng đang trống.</p>}</section>}</div>
      {showBack && <button className="store-header-back" type="button" onClick={() => navigate(-1)}><ArrowBackOutlinedIcon />Quay lại</button>}
    </nav>
  </header>;
}
