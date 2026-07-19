import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import AddShoppingCartOutlinedIcon from "@mui/icons-material/AddShoppingCartOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import CheckroomOutlinedIcon from "@mui/icons-material/CheckroomOutlined";
import ChevronLeftOutlinedIcon from "@mui/icons-material/ChevronLeftOutlined";
import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import DevicesOtherOutlinedIcon from "@mui/icons-material/DevicesOtherOutlined";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import RemoveOutlinedIcon from "@mui/icons-material/RemoveOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { getCategories } from "../services/categoryService";
import { getProducts } from "../services/productService";
import { loadWishlist, toggleWishlist } from "../services/wishlistService";
import { addCartItem, getMyCart, removeCartItem, updateCartItem } from "../services/cartService";
import { isAuthenticated } from "../services/authenticationService";
import ShopStoreHeader from "../components/ShopStoreHeader";
import "./Shop.css";
import "./ShopDense.css";
import "./ShopFlashSale.css";

const formatPrice = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
const productUrl = (product) => `/shop/products/${encodeURIComponent((product.name || "san-pham").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, ""))}-${product.id}`;
const categoryUrl = (category) => `/shop/categories/${encodeURIComponent((category.name || "danh-muc").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, ""))}-${category.id}`;

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

export default function Shop() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [cart, setCart] = useState([]);
  const [wishlist, setWishlist] = useState([]);
  const [wishlistLoading, setWishlistLoading] = useState(true);
  const [wishlistPendingIds, setWishlistPendingIds] = useState(() => new Set());
  const [wishlistNotice, setWishlistNotice] = useState("");

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
    if (!isAuthenticated()) {
      setWishlist([]);
      setWishlistLoading(false);
      return undefined;
    }
    let active = true;
    loadWishlist().then((items) => {
      if (active) setWishlist(items);
    }).catch((error) => {
      if (active) setWishlistNotice(error.response?.data?.message || "Không thể tải danh sách yêu thích.");
    }).finally(() => {
      if (active) setWishlistLoading(false);
    });
    return () => { active = false; };
  }, []);
  useEffect(() => {
    if (!isAuthenticated()) return;
    getMyCart().then((data) => setCart(data.items || [])).catch(() => setCart([]));
  }, []);
  const changeWishlist = async (event, product) => {
    event.preventDefault();
    event.stopPropagation();
    if (!isAuthenticated()) return navigate("/shop/login?redirect=/shop");
    if (wishlistLoading || wishlistPendingIds.has(product.id)) return;

    setWishlistPendingIds((current) => new Set(current).add(product.id));
    try {
      const result = await toggleWishlist(product, wishlist);
      setWishlist(result.items);
      setWishlistNotice("");
    } catch (error) {
      setWishlistNotice(error.response?.data?.message || "Không thể cập nhật yêu thích.");
    } finally {
      setWishlistPendingIds((current) => {
        const next = new Set(current);
        next.delete(product.id);
        return next;
      });
    }
  };
  const visibleProducts = products;
  const addToCart = async (product) => {
    if (!isAuthenticated()) return navigate("/shop/login?redirect=/shop");
    try { setCart((await addCartItem({ productId: product.id, quantity: 1 })).items || []); } catch { /* keep browsing when Cart Service is temporarily unavailable */ }
  };
  const updateQuantity = async (item, change) => {
    try {
      const next = Number(item.quantity || 1) + change <= 0
        ? await removeCartItem(item.id)
        : await updateCartItem(item.id, { quantity: Number(item.quantity || 1) + change, selected: item.selected });
      setCart(next.items || []);
    } catch { /* cart remains unchanged */ }
  };
  const removeFromCart = async (itemId) => {
    try { setCart((await removeCartItem(itemId)).items || []); } catch { /* cart remains unchanged */ }
  };
  const total = cart.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0);
  const imageUrl = (product) => product.imageUrl || product.images?.find((image) => image.isPrimary)?.url || product.images?.[0]?.url;

  return <><ShopStoreHeader />{wishlistNotice && <p className="shop-wishlist-notice" role="alert">{wishlistNotice}</p>}<div className="shop-shell">
    <main className="shop-main"><section className="shop-hero"><div><span className="shop-hero-tag">✦ Bộ sưu tập mới</span><h1>Tìm phong cách <em>của riêng bạn</em></h1><p>Khám phá thời trang, làm đẹp và công nghệ được tuyển chọn cho bạn.</p><button onClick={() => document.getElementById("best-deals")?.scrollIntoView({ behavior: "smooth" })}>Mua ngay →</button></div><div className="shop-hero-visual"><ShoppingBagOutlinedIcon /></div></section>
      <section className="shop-category-strip">{categories.slice(0, 6).map((category, index) => <button key={category.id} onClick={() => navigate(categoryUrl(category))}><span>{index % 3 === 0 ? <CheckroomOutlinedIcon /> : index % 3 === 1 ? <DevicesOtherOutlinedIcon /> : <CategoryOutlinedIcon />}</span>{category.name}</button>)}{categories.length === 0 && ["Thời trang", "Làm đẹp", "Điện tử", "Nhà cửa", "Thể thao"].map((name) => <button key={name}><span><CategoryOutlinedIcon /></span>{name}</button>)}</section>
      <section className="shop-promo-row"><article><small>FLASH SALE</small><strong>Giảm đến 70%</strong><button>Mua ngay →</button></article><article><small>MIỄN PHÍ VẬN CHUYỂN</small><strong>Đơn từ 500.000đ</strong><button>Mua ngay →</button></article><article><small>HÀNG MỚI VỀ</small><strong>Xu hướng mới nhất</strong><button>Mua ngay →</button></article></section>
      <section id="best-deals"><div className="shop-section-head"><div><small>Được chọn cho bạn</small><h2>Ưu đãi tốt nhất</h2></div><button onClick={() => navigate("/search")}>Xem tất cả →</button></div><div className="shop-product-grid">{visibleProducts.slice(0, 8).map((product, index) => <article className="shop-product-card" key={product.id}><div className="shop-product-media">{index < 4 && <span>-{10 + index * 5}%</span>}<button type="button" className={wishlist.some((item) => item.productId === product.id) ? "is-wishlisted" : ""} disabled={wishlistLoading || wishlistPendingIds.has(product.id)} onClick={(event) => changeWishlist(event, product)} aria-label="Thêm hoặc xoá yêu thích"><FavoriteBorderOutlinedIcon /></button>{imageUrl(product) ? <img src={imageUrl(product)} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><div className="shop-product-body"><small>{product.categoryName || "Sản phẩm"}</small><h3>{product.name}</h3><strong>{formatPrice(product.price)}</strong><div><span><StarRoundedIcon /> 4.8</span><button onClick={() => addToCart(product)} aria-label={`Thêm ${product.name} vào giỏ`}><AddShoppingCartOutlinedIcon /></button></div></div></article>)}</div>{visibleProducts.length === 0 && <div className="shop-empty">Chưa có sản phẩm phù hợp.</div>}</section>
      <section className="shop-recommendations"><div className="shop-section-head"><div><small>Khám phá thêm</small><h2>Gợi ý dành cho bạn</h2></div><button onClick={() => navigate("/search")}>Xem tất cả →</button></div><div className="shop-product-grid">{visibleProducts.slice(8, 16).map((product, index) => <article className="shop-product-card" key={`recommended-${product.id}`}><div className="shop-product-media">{index < 3 && <span>Mới</span>}<button type="button" className={wishlist.some((item) => item.productId === product.id) ? "is-wishlisted" : ""} disabled={wishlistLoading || wishlistPendingIds.has(product.id)} onClick={(event) => changeWishlist(event, product)} aria-label="Thêm hoặc xoá yêu thích"><FavoriteBorderOutlinedIcon /></button>{imageUrl(product) ? <img src={imageUrl(product)} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><div className="shop-product-body"><small>{product.categoryName || "Sản phẩm"}</small><h3>{product.name}</h3><strong>{formatPrice(product.price)}</strong><div><span><StarRoundedIcon /> 4.8</span><button onClick={() => addToCart(product)} aria-label={`Thêm ${product.name} vào giỏ`}><AddShoppingCartOutlinedIcon /></button></div></div></article>)}</div></section>
      <section className="shop-trust"><div><ShoppingBagOutlinedIcon /><p><b>Thanh toán an toàn</b>Bảo mật 100%</p></div><div><LocalOfferOutlinedIcon /><p><b>Đổi trả dễ dàng</b>Trong vòng 30 ngày</p></div><div><NotificationsNoneOutlinedIcon /><p><b>Hỗ trợ 24/7</b>Luôn sẵn sàng</p></div></section>
      <ShopFlashSale products={products} onOpen={(product) => navigate(productUrl(product))} />
    </main>
    <aside className="shop-cart"><div className="shop-cart-card"><div className="shop-cart-head"><h3>Giỏ hàng ({cart.reduce((sum, item) => sum + item.quantity, 0)})</h3><CloseOutlinedIcon /></div>{cart.length === 0 ? <div className="shop-cart-empty"><ShoppingBagOutlinedIcon /><p>Giỏ hàng đang trống.</p><small>Thêm sản phẩm để xem tổng tiền.</small></div> : <><div>{cart.map((item) => <div className="shop-cart-line" key={item.id}><div>{imageUrl(item) ? <img src={imageUrl(item)} alt="" /> : <ShoppingBagOutlinedIcon />}</div><section><b>{item.productName}</b><small>{formatPrice(item.price)}</small><p><button onClick={() => updateQuantity(item, -1)}><RemoveOutlinedIcon /></button>{item.quantity}<button onClick={() => updateQuantity(item, 1)}><AddShoppingCartOutlinedIcon /></button></p></section><button onClick={() => removeFromCart(item.id)}><DeleteOutlineOutlinedIcon /></button></div>)}</div><div className="shop-cart-summary"><p><span>Tạm tính</span><b>{formatPrice(total)}</b></p><p><span>Vận chuyển</span><b>Miễn phí</b></p><p className="total"><span>Tổng cộng</span><b>{formatPrice(total)}</b></p><button onClick={() => navigate("/cart")}>Thanh toán ({cart.reduce((sum, item) => sum + item.quantity, 0)})</button></div></>}</div></aside>
  </div></>;
}
