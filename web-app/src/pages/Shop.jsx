import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AddShoppingCartOutlinedIcon from "@mui/icons-material/AddShoppingCartOutlined";
import AppsOutlinedIcon from "@mui/icons-material/AppsOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import CheckroomOutlinedIcon from "@mui/icons-material/CheckroomOutlined";
import ChevronLeftOutlinedIcon from "@mui/icons-material/ChevronLeftOutlined";
import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import DevicesOtherOutlinedIcon from "@mui/icons-material/DevicesOtherOutlined";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import RemoveOutlinedIcon from "@mui/icons-material/RemoveOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { getCategories } from "../services/categoryService";
import { getProducts, searchProductSuggestions } from "../services/productService";
import { isAuthenticated, logout } from "../services/authenticationService";
import { getWishlist, toggleWishlist } from "../services/wishlistService";
import "./Shop.css";
import "./ShopDense.css";
import "./ShopFlashSale.css";
import "./ShopLandingSuggestions.css";

const formatPrice = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
const readShopCart = () => { try { const saved = JSON.parse(sessionStorage.getItem("nova-shop-cart") || "[]"); return Array.isArray(saved) ? saved : []; } catch { return []; } };
const productUrl = (product) => `/shop/products/${encodeURIComponent((product.name || "san-pham").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, ""))}-${product.id}`;
const categoryUrl = (category) => `/shop/categories/${encodeURIComponent((category.name || "danh-muc").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, ""))}-${category.id}`;
const navItems = [[HomeOutlinedIcon, "Trang chủ"], [CategoryOutlinedIcon, "Danh mục"], [LocalOfferOutlinedIcon, "Ưu đãi"], [AppsOutlinedIcon, "Hàng mới"]];

function ShopFlashSale({ products, onOpen }) {
  const [now, setNow] = useState(() => new Date());
  const [start, setStart] = useState(0);
  useEffect(() => { const timer = window.setInterval(() => setNow(new Date()), 1000); return () => window.clearInterval(timer); }, []);
  const countdown = useMemo(() => {
    const target = new Date(now); const slots = [0, 8, 18];
    const nextHour = slots.find((hour) => hour > now.getHours() || (hour === now.getHours() && now.getMinutes() === 0 && now.getSeconds() === 0));
    if (nextHour === undefined) target.setDate(target.getDate() + 1);
    target.setHours(nextHour ?? 0, 0, 0, 0);
    const seconds = Math.max(0, Math.floor((target.getTime() - now.getTime()) / 1000));
    return [Math.floor(seconds / 3600), Math.floor(seconds / 60) % 60, seconds % 60].map((item) => String(item).padStart(2, "0"));
  }, [now]);
  const viewSize = 7;
  const shown = products.length ? Array.from({ length: Math.min(viewSize, products.length) }, (_, index) => products[(start + index) % products.length]) : [];
  const move = (step) => setStart((current) => products.length ? (current + step + products.length) % products.length : 0);
  return <section className="shop-flash-sale"><header><div><LocalOfferOutlinedIcon /><b>FLASH SALE</b><time>{countdown.map((part, index) => <i key={index}>{part}</i>)}</time></div><button onClick={() => document.getElementById("best-deals")?.scrollIntoView({ behavior: "smooth" })}>Xem tất cả ›</button></header><div className="shop-flash-carousel"><button className="shop-flash-arrow left" aria-label="Sản phẩm Flash Sale trước" onClick={() => move(-viewSize)}><ChevronLeftOutlinedIcon /></button><div className="shop-flash-sale-list">{shown.map((product, index) => <article key={`${product.id}-${index}`} onClick={() => onOpen(product)}><div className="shop-flash-image"><span>−{20 + index * 7}%</span>{product.images?.find((image) => image.isPrimary)?.url || product.images?.[0]?.url ? <img src={product.images?.find((image) => image.isPrimary)?.url || product.images?.[0]?.url} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><strong>{formatPrice(product.price)}</strong><div><i style={{ width: `${34 + index * 9}%` }} />SELLING FAST</div></article>)}</div><button className="shop-flash-arrow right" aria-label="Sản phẩm Flash Sale tiếp theo" onClick={() => move(viewSize)}><ChevronRightOutlinedIcon /></button></div></section>;
}

function ShopLandingSuggestions({ query, onOpen }) {
  const [items, setItems] = useState([]);
  useEffect(() => { const timer = window.setTimeout(() => searchProductSuggestions(query).then(setItems).catch(() => setItems([])), 300); return () => window.clearTimeout(timer); }, [query]);
  if (!items.length) return null;
  return <div className="shop-landing-suggestions">{items.map((item) => <button key={item.productId} onClick={() => onOpen(item)}>{item.thumbnailUrl && <img src={item.thumbnailUrl} alt=""/>}<span>{item.name}<small>{formatPrice(item.price)}</small></span></button>)}</div>;
}

export default function Shop() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [cart, setCart] = useState(readShopCart);
  const [query, setQuery] = useState("");
  const [wishlist, setWishlist] = useState(getWishlist);
  const loggedIn = isAuthenticated();

  useEffect(() => { Promise.all([getProducts({ page: 1, size: 12 }), getCategories()]).then(([productData, categoryData]) => { setProducts(productData.content || []); setCategories(categoryData || []); }).catch(() => {}); }, []);
  useEffect(() => {
    const openProductDetail = (event) => {
      const card = event.target.closest(".shop-product-card");
      if (!card || event.target.closest("button")) return;
      const name = card.querySelector(".shop-product-body h3")?.textContent;
      const product = products.find((item) => item.name === name);
      if (product) navigate(productUrl(product));
    };
    document.addEventListener("click", openProductDetail);
    return () => document.removeEventListener("click", openProductDetail);
  }, [navigate, products]);
  useEffect(() => {
    const openWishlist = (event) => {
      const button = event.target.closest(".shop-sidebar .shop-nav");
      if (button?.textContent?.includes("Yêu thích")) navigate("/shop/wishlist");
    };
    document.addEventListener("click", openWishlist);
    return () => document.removeEventListener("click", openWishlist);
  }, [navigate]);
  useEffect(() => {
    const openCart = (event) => {
      if (event.target.closest(".shop-side-promo")) navigate("/cart");
    };
    document.addEventListener("click", openCart);
    return () => document.removeEventListener("click", openCart);
  }, [navigate]);
  useEffect(() => {
    const openHotDeals = (event) => {
      const buttons = [...document.querySelectorAll(".shop-sidebar .shop-nav")];
      if (event.target.closest(".shop-sidebar .shop-nav") === buttons[2]) navigate("/shop/hot-deals");
    };
    document.addEventListener("click", openHotDeals);
    return () => document.removeEventListener("click", openHotDeals);
  }, [navigate]);
  useEffect(() => {
    document.querySelectorAll(".shop-product-card").forEach((card) => {
      const name = card.querySelector(".shop-product-body h3")?.textContent;
      const product = products.find((item) => item.name === name);
      card.querySelector(".shop-product-media > button")?.classList.toggle("is-wishlisted", Boolean(product && wishlist.some((item) => item.id === product.id)));
    });
  }, [products, wishlist]);
  useEffect(() => { sessionStorage.setItem("nova-shop-cart", JSON.stringify(cart)); }, [cart]);
  useEffect(() => {
    const handleWishlist = (event) => {
      const productButton = event.target.closest(".shop-product-media > button");
      const wishlistButton = event.target.closest("button[aria-label='Sản phẩm yêu thích']");
      if (wishlistButton) { navigate("/shop/wishlist"); return; }
      if (!productButton) return;
      event.preventDefault();
      event.stopPropagation();
      const card = productButton.closest(".shop-product-card");
      const name = card?.querySelector(".shop-product-body h3")?.textContent;
      const product = products.find((item) => item.name === name);
      if (product) {
        const next = toggleWishlist(product);
        setWishlist(next);
        productButton.classList.toggle("is-wishlisted", next.some((item) => item.id === product.id));
      }
    };
    document.addEventListener("click", handleWishlist, true);
    return () => document.removeEventListener("click", handleWishlist, true);
  }, [navigate, products]);
  const visibleProducts = products;
  const addToCart = (product) => setCart((current) => { const found = current.find((item) => item.id === product.id); return found ? current.map((item) => item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item) : [...current, { ...product, quantity: 1 }]; });
  const updateQuantity = (id, change) => setCart((current) => current.flatMap((item) => item.id === id ? (item.quantity + change > 0 ? [{ ...item, quantity: item.quantity + change }] : []) : [item]));
  const total = cart.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0);
  const imageUrl = (product) => product.images?.find((image) => image.isPrimary)?.url || product.images?.[0]?.url;

  return <div className="shop-shell">
    <aside className="shop-sidebar"><Link className="shop-brand" to="/shop"><span>N</span>NovaShop</Link><nav>{navItems.map(([Icon, label], index) => <button className={`shop-nav ${index === 0 ? "active" : ""}`} key={label}><Icon />{label}</button>)}<div className="shop-nav-divider" /><button className="shop-nav" onClick={() => navigate("/shop/orders")}><ShoppingBagOutlinedIcon />Đơn hàng</button><button className="shop-nav"><FavoriteBorderOutlinedIcon />Yêu thích</button><button className="shop-nav" onClick={() => navigate("/shop/account")}><PersonOutlineOutlinedIcon />Tài khoản</button></nav><div className="shop-side-promo"><small>ƯU ĐÃI ĐẶC BIỆT</small><h3>Sale mùa hè -50%</h3><p>Áp dụng cho danh mục thời trang.</p><button>Mua ngay</button></div></aside>
    <header className="shop-topbar"><label className="shop-search"><SearchOutlinedIcon /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm sản phẩm, thương hiệu..." /></label>{loggedIn ? <div className="shop-top-actions"><button aria-label="Sản phẩm yêu thích"><FavoriteBorderOutlinedIcon /></button><button aria-label="Thông báo"><NotificationsNoneOutlinedIcon /><b>3</b></button><button className="shop-logout" onClick={() => { logout(); window.location.assign("/shop"); }}>Đăng xuất</button></div> : <div className="shop-guest-actions"><Link to="/shop/register">Đăng ký</Link><span>|</span><Link to="/shop/login">Đăng nhập</Link></div>}</header>
    <main className="shop-main"><section className="shop-hero"><div><span className="shop-hero-tag">✦ Bộ sưu tập mới</span><h1>Tìm phong cách <em>của riêng bạn</em></h1><p>Khám phá thời trang, làm đẹp và công nghệ được tuyển chọn cho bạn.</p><button onClick={() => document.getElementById("best-deals")?.scrollIntoView({ behavior: "smooth" })}>Mua ngay →</button></div><div className="shop-hero-visual"><ShoppingBagOutlinedIcon /></div></section>
      <section className="shop-category-strip">{categories.slice(0, 6).map((category, index) => <button key={category.id} onClick={() => navigate(categoryUrl(category))}><span>{index % 3 === 0 ? <CheckroomOutlinedIcon /> : index % 3 === 1 ? <DevicesOtherOutlinedIcon /> : <CategoryOutlinedIcon />}</span>{category.name}</button>)}{categories.length === 0 && ["Thời trang", "Làm đẹp", "Điện tử", "Nhà cửa", "Thể thao"].map((name) => <button key={name}><span><CategoryOutlinedIcon /></span>{name}</button>)}</section>
      <section className="shop-promo-row"><article><small>FLASH SALE</small><strong>Giảm đến 70%</strong><button>Mua ngay →</button></article><article><small>MIỄN PHÍ VẬN CHUYỂN</small><strong>Đơn từ 500.000đ</strong><button>Mua ngay →</button></article><article><small>HÀNG MỚI VỀ</small><strong>Xu hướng mới nhất</strong><button>Mua ngay →</button></article></section>
      <section id="best-deals"><div className="shop-section-head"><div><small>Được chọn cho bạn</small><h2>Ưu đãi tốt nhất</h2></div><button onClick={() => navigate("/search")}>Xem tất cả →</button></div><div className="shop-product-grid">{visibleProducts.slice(0, 8).map((product, index) => <article className="shop-product-card" key={product.id}><div className="shop-product-media">{index < 4 && <span>-{10 + index * 5}%</span>}<button><FavoriteBorderOutlinedIcon /></button>{imageUrl(product) ? <img src={imageUrl(product)} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><div className="shop-product-body"><small>{product.categoryName || "Sản phẩm"}</small><h3>{product.name}</h3><strong>{formatPrice(product.price)}</strong><div><span><StarRoundedIcon /> 4.8</span><button onClick={() => addToCart(product)} aria-label={`Thêm ${product.name} vào giỏ`}><AddShoppingCartOutlinedIcon /></button></div></div></article>)}</div>{visibleProducts.length === 0 && <div className="shop-empty">Chưa có sản phẩm phù hợp.</div>}</section>
      <section className="shop-recommendations"><div className="shop-section-head"><div><small>Khám phá thêm</small><h2>Gợi ý dành cho bạn</h2></div><button onClick={() => navigate("/search")}>Xem tất cả →</button></div><div className="shop-product-grid">{visibleProducts.slice(8, 16).map((product, index) => <article className="shop-product-card" key={`recommended-${product.id}`}><div className="shop-product-media">{index < 3 && <span>Mới</span>}<button><FavoriteBorderOutlinedIcon /></button>{imageUrl(product) ? <img src={imageUrl(product)} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><div className="shop-product-body"><small>{product.categoryName || "Sản phẩm"}</small><h3>{product.name}</h3><strong>{formatPrice(product.price)}</strong><div><span><StarRoundedIcon /> 4.8</span><button onClick={() => addToCart(product)} aria-label={`Thêm ${product.name} vào giỏ`}><AddShoppingCartOutlinedIcon /></button></div></div></article>)}</div></section>
      <section className="shop-trust"><div><ShoppingBagOutlinedIcon /><p><b>Thanh toán an toàn</b>Bảo mật 100%</p></div><div><LocalOfferOutlinedIcon /><p><b>Đổi trả dễ dàng</b>Trong vòng 30 ngày</p></div><div><NotificationsNoneOutlinedIcon /><p><b>Hỗ trợ 24/7</b>Luôn sẵn sàng</p></div></section>
      <ShopFlashSale products={products} onOpen={(product) => navigate(productUrl(product))} />
    </main>
    <aside className="shop-cart"><div className="shop-cart-card"><div className="shop-cart-head"><h3>Giỏ hàng ({cart.reduce((sum, item) => sum + item.quantity, 0)})</h3><CloseOutlinedIcon /></div>{cart.length === 0 ? <div className="shop-cart-empty"><ShoppingBagOutlinedIcon /><p>Giỏ hàng đang trống.</p><small>Thêm sản phẩm để xem tổng tiền.</small></div> : <><div>{cart.map((item) => <div className="shop-cart-line" key={item.id}><div>{imageUrl(item) ? <img src={imageUrl(item)} alt="" /> : <ShoppingBagOutlinedIcon />}</div><section><b>{item.name}</b><small>{formatPrice(item.price)}</small><p><button onClick={() => updateQuantity(item.id, -1)}><RemoveOutlinedIcon /></button>{item.quantity}<button onClick={() => updateQuantity(item.id, 1)}><AddShoppingCartOutlinedIcon /></button></p></section><button onClick={() => setCart((items) => items.filter((cartItem) => cartItem.id !== item.id))}><DeleteOutlineOutlinedIcon /></button></div>)}</div><div className="shop-cart-summary"><p><span>Tạm tính</span><b>{formatPrice(total)}</b></p><p><span>Vận chuyển</span><b>Miễn phí</b></p><p className="total"><span>Tổng cộng</span><b>{formatPrice(total)}</b></p><button onClick={() => navigate("/cart")}>Thanh toán ({cart.reduce((sum, item) => sum + item.quantity, 0)})</button></div></>}</div></aside>
    <ShopLandingSuggestions query={query} onOpen={(product) => navigate(productUrl({ ...product, id: product.productId }))} />
  </div>;
}

