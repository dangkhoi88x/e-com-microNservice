import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import AddShoppingCartOutlinedIcon from "@mui/icons-material/AddShoppingCartOutlined";
import AccessTimeOutlinedIcon from "@mui/icons-material/AccessTimeOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import CheckroomOutlinedIcon from "@mui/icons-material/CheckroomOutlined";
import ChairOutlinedIcon from "@mui/icons-material/ChairOutlined";
import ChevronLeftOutlinedIcon from "@mui/icons-material/ChevronLeftOutlined";
import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import PhoneInTalkOutlinedIcon from "@mui/icons-material/PhoneInTalkOutlined";
import PhoneIphoneOutlinedIcon from "@mui/icons-material/PhoneIphoneOutlined";
import FaceRetouchingNaturalOutlinedIcon from "@mui/icons-material/FaceRetouchingNaturalOutlined";
import RemoveOutlinedIcon from "@mui/icons-material/RemoveOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { getCategories } from "../services/categoryService";
import { getProducts } from "../services/productService";
import { loadWishlist, toggleWishlist } from "../services/wishlistService";
import { addCartItem, getMyCart, removeCartItem, updateCartItem } from "../services/cartService";
import { isAuthenticated } from "../services/authenticationService";
import { getActiveProductSales, getFlashDealNotificationSubscriptions, getLiveFlashDeals, getUpcomingFlashDeals, subscribeFlashDealNotification } from "../services/promotionService";
import ShopStoreHeader from "../components/ShopStoreHeader";
import "./Shop.css";
import "./ShopDense.css";
import "./ShopFlashSale.css";
import "./ShopFlashSaleStates.css";
import "./ShopProductGrid.css";

const formatPrice = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
const productUrl = (product) => `/shop/products/${encodeURIComponent((product.name || "san-pham").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, ""))}-${product.id}`;
const categoryUrl = (category) => `/shop/categories/${encodeURIComponent((category.name || "danh-muc").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, ""))}-${category.id}`;
const normalizedCategoryName = (category) => ["skincare", "cham-soc-da"].includes(category.slug) ? "Chăm sóc da" : category.name;
const categoryIcon = (category) => ({ "dien-thoai": <PhoneIphoneOutlinedIcon />, "dien-thoai-ban": <PhoneInTalkOutlinedIcon />, "thoi-trang": <CheckroomOutlinedIcon />, "skincare": <FaceRetouchingNaturalOutlinedIcon />, "cham-soc-da": <FaceRetouchingNaturalOutlinedIcon />, "nha-cua": <ChairOutlinedIcon /> }[category.slug] || <CategoryOutlinedIcon />);
const productImageUrl = (product) => product?.imageUrl || product?.images?.find((image) => image.isPrimary)?.url || product?.images?.[0]?.url;
const shuffle = (items) => {
  const result = [...items];
  for (let index = result.length - 1; index > 0; index -= 1) {
    const target = Math.floor(Math.random() * (index + 1));
    [result[index], result[target]] = [result[target], result[index]];
  }
  return result;
};

const recommendFromCategories = (products, limit = 12, perCategory = 3) => {
  const eligibleProducts = products.filter((product) =>
    product.status === "ACTIVE" && Number(product.quantity || 0) > 0
  );
  const grouped = new Map();

  eligibleProducts.forEach((product) => {
    const categoryKey = product.categoryId || product.categoryName || "other";
    if (!grouped.has(categoryKey)) grouped.set(categoryKey, []);
    grouped.get(categoryKey).push(product);
  });

  const recommendations = shuffle([...grouped.values()])
    .flatMap((categoryProducts) => shuffle(categoryProducts).slice(0, perCategory));

  if (recommendations.length < limit) {
    const selectedIds = new Set(recommendations.map((product) => product.id));
    recommendations.push(...shuffle(eligibleProducts.filter((product) => !selectedIds.has(product.id))));
  }

  return shuffle(recommendations).slice(0, limit);
};
function ProductImage({ product }) { const source = productImageUrl(product); const [failed, setFailed] = useState(!source); return failed ? <div className="shop-product-placeholder" aria-label="Ảnh sản phẩm NovaShop">N</div> : <img src={source} alt={product.name} onError={() => setFailed(true)} />; }

function ShopFlashSale({ products, deals, upcomingDeals, notifiedIds, onNotify, onOpen }) {
  const [now, setNow] = useState(() => new Date()); const [start, setStart] = useState(0); const [notified, setNotified] = useState(false);
  useEffect(() => { if (!deals.length && !upcomingDeals.length) return undefined; const timer = window.setInterval(() => setNow(new Date()), 1000); return () => window.clearInterval(timer); }, [deals.length, upcomingDeals.length]);
  const live = deals.length > 0; const upcoming = !live && upcomingDeals.length > 0; const targetAt = live ? deals.map((deal) => new Date(deal.endAt)).sort((a, b) => a - b)[0] : upcoming ? upcomingDeals.map((deal) => new Date(deal.startAt)).sort((a, b) => a - b)[0] : null;
  const countdown = useMemo(() => { if (!targetAt) return []; const seconds = Math.max(0, Math.floor((targetAt.getTime() - now.getTime()) / 1000)); return [Math.floor(seconds / 3600), Math.floor(seconds / 60) % 60, seconds % 60].map((item) => String(item).padStart(2, "0")); }, [now, targetAt]);
  const dealProducts = deals.flatMap((deal) => (deal.items || []).map((item) => ({ deal, item, product: products.find((product) => product.id === item.productId) }))).filter((entry) => entry.product);
  const viewSize = 7;
  const shown = dealProducts.length ? Array.from({ length: Math.min(viewSize, dealProducts.length) }, (_, index) => dealProducts[(start + index) % dealProducts.length]) : [];
  const move = (step) => setStart((current) => dealProducts.length ? (current + step + dealProducts.length) % dealProducts.length : 0);
  if (!live && !upcoming) return <section className="shop-flash-sale shop-flash-empty-state"><div className="shop-flash-empty-art"><AccessTimeOutlinedIcon /></div><div><b>SẮP CÓ ƯU ĐÃI MỚI</b><h2>Flash Sale mới sắp diễn ra</h2><p>Đăng ký nhận thông báo để không bỏ lỡ deal tốt tiếp theo.</p><button type="button" onClick={() => setNotified(true)}>{notified ? "Đã đăng ký ✓" : "Notify me"}</button></div></section>;
  return <section className={`shop-flash-sale ${upcoming ? "is-upcoming" : "is-live"}`}><header><div><LocalOfferOutlinedIcon /><b>{upcoming ? "FLASH SALE SẮP DIỄN RA" : "FLASH SALE"}</b><time aria-label={upcoming ? "Thời gian bắt đầu" : "Thời gian kết thúc"}>{countdown.map((part, index) => <i key={index}>{part}</i>)}</time></div>{upcoming ? <button onClick={() => onNotify(upcomingDeals[0].id)}>{notifiedIds.has(upcomingDeals[0].id) ? "Đã đăng ký ✓" : "Notify me"}</button> : <button onClick={() => document.getElementById("best-deals")?.scrollIntoView({ behavior: "smooth" })}>Xem ngay ›</button>}</header>{shown.length ? <div className="shop-flash-carousel"><button className="shop-flash-arrow left" aria-label="Sản phẩm Flash Sale trước" onClick={() => move(-viewSize)}><ChevronLeftOutlinedIcon /></button><div className="shop-flash-sale-list">{shown.map(({ product, item }, index) => <article key={`${item.id}-${index}`} onClick={() => onOpen(product)}><div className="shop-flash-image"><span>−{item.discountPercent || Math.round((1 - Number(item.salePrice) / Number(item.originalPrice)) * 100)}%</span>{product.images?.find((image) => image.isPrimary)?.url || product.images?.[0]?.url ? <img src={product.images?.find((image) => image.isPrimary)?.url || product.images?.[0]?.url} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><strong>{formatPrice(item.salePrice)}</strong><del>{formatPrice(item.originalPrice)}</del><div><i style={{ width: `${Math.min(100, Math.max(8, 100 - Number(item.quota || 0)))}%` }} />CÒN {item.quota} SUẤT</div></article>)}</div><button className="shop-flash-arrow right" aria-label="Sản phẩm Flash Sale tiếp theo" onClick={() => move(viewSize)}><ChevronRightOutlinedIcon /></button></div> : <div className="shop-flash-upcoming-copy"><AccessTimeOutlinedIcon /><span>Deal đã lên lịch — chuẩn bị săn sale nhé!</span></div>}</section>;
}

export default function Shop() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [flashDeals, setFlashDeals] = useState([]);
  const [activeSales, setActiveSales] = useState([]);
  const [upcomingFlashDeals, setUpcomingFlashDeals] = useState([]);
  const [flashNotificationIds, setFlashNotificationIds] = useState(() => new Set());
  const [categories, setCategories] = useState([]);
  const [cart, setCart] = useState([]);
  const [wishlist, setWishlist] = useState([]);
  const [wishlistLoading, setWishlistLoading] = useState(true);
  const [wishlistPendingIds, setWishlistPendingIds] = useState(() => new Set());
  const [wishlistNotice, setWishlistNotice] = useState("");

  useEffect(() => { Promise.all([getProducts({ page: 1, size: 100 }), getCategories()]).then(([productData, categoryData]) => { setProducts(productData.content || []); setCategories(categoryData || []); }).catch(() => {}); }, []);
  useEffect(() => { getLiveFlashDeals().then(setFlashDeals).catch(() => setFlashDeals([])); getActiveProductSales().then(setActiveSales).catch(() => setActiveSales([])); getUpcomingFlashDeals().then(setUpcomingFlashDeals).catch(() => setUpcomingFlashDeals([])); }, []);
  useEffect(() => { if (isAuthenticated()) getFlashDealNotificationSubscriptions().then((ids) => setFlashNotificationIds(new Set(ids))).catch(() => {}); }, []);
  const notifyFlashSale = async (flashDealId) => { if (!isAuthenticated()) return navigate("/shop/login?redirect=/shop"); try { await subscribeFlashDealNotification(flashDealId); setFlashNotificationIds((ids) => new Set(ids).add(flashDealId)); } catch (error) { setWishlistNotice(error.response?.data?.message || "Không thể đăng ký thông báo Flash Sale."); } };
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
  const recommendedProducts = useMemo(() => recommendFromCategories(products), [products]);
  const saleProducts = useMemo(() => { const best = new Map(); activeSales.forEach((deal) => (deal.items || []).forEach((item) => { if (item.quotaLimited !== false && Number(item.quota || 0) <= 0) return; const product = products.find((entry) => entry.id === item.productId); if (!product || Number(item.salePrice) >= Number(item.originalPrice)) return; const candidate = { ...product, salePrice: Number(item.salePrice), originalPrice: Number(item.originalPrice), discountPercent: Number(item.discountPercent || (100 - Number(item.salePrice) * 100 / Number(item.originalPrice))), saleType: deal.saleType || "FLASH" }; const current = best.get(product.id); if (!current || candidate.salePrice < current.salePrice || (candidate.salePrice === current.salePrice && candidate.saleType === "FLASH" && current.saleType !== "FLASH")) best.set(product.id, candidate); })); return [...best.values()].sort((a, b) => b.discountPercent - a.discountPercent); }, [activeSales, products]);
  const visibleCategories = useMemo(() => { const seen = new Set(); return categories.filter((category) => !/san pham abcx/i.test(category.name)).filter((category) => { const key = normalizedCategoryName(category).toLocaleLowerCase("vi-VN"); if (seen.has(key)) return false; seen.add(key); return true; }); }, [categories]);
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
  const imageUrl = productImageUrl;

  return <><ShopStoreHeader />{wishlistNotice && <p className="shop-wishlist-notice" role="alert">{wishlistNotice}</p>}<div className="shop-shell">
    <main className="shop-main"><section className="shop-hero"><div><span className="shop-hero-tag">✦ Bộ sưu tập mới</span><h1>Tìm phong cách <em>của riêng bạn</em></h1><p>Khám phá thời trang, làm đẹp và công nghệ được tuyển chọn cho bạn.</p><button onClick={() => document.getElementById("best-deals")?.scrollIntoView({ behavior: "smooth" })}>Mua ngay →</button></div><div className="shop-hero-visual"><ShoppingBagOutlinedIcon /></div></section>
      <section className="shop-category-strip">{visibleCategories.slice(0, 6).map((category) => <button key={category.id} onClick={() => navigate(categoryUrl(category))}><span>{categoryIcon(category)}</span>{normalizedCategoryName(category)}</button>)}{visibleCategories.length === 0 && ["Thời trang", "Làm đẹp", "Điện tử", "Nhà cửa", "Thể thao"].map((name) => <button key={name}><span><CategoryOutlinedIcon /></span>{name}</button>)}</section>
      <section className="shop-promo-row"><article><small>FLASH SALE</small><strong>Giảm đến 70%</strong><button>Mua ngay →</button></article><article><small>MIỄN PHÍ VẬN CHUYỂN</small><strong>Đơn từ 500.000đ</strong><button>Mua ngay →</button></article><article><small>HÀNG MỚI VỀ</small><strong>Xu hướng mới nhất</strong><button>Mua ngay →</button></article></section>
      <section id="best-deals"><div className="shop-section-head"><div><small>Được chọn cho bạn</small><h2>Ưu đãi tốt nhất</h2></div><button onClick={() => navigate("/shop/best-deals")}>Xem tất cả →</button></div><div className="shop-product-grid">{saleProducts.slice(0, 8).map((product) => <article className="shop-product-card" key={product.id}><div className="shop-product-media"><span>-{Math.round(product.discountPercent)}%</span><button type="button" className={wishlist.some((item) => item.productId === product.id) ? "is-wishlisted" : ""} disabled={wishlistLoading || wishlistPendingIds.has(product.id)} onClick={(event) => changeWishlist(event, product)} aria-label="Thêm hoặc xoá yêu thích"><FavoriteBorderOutlinedIcon /></button><ProductImage product={product} /></div><div className="shop-product-body"><small>{product.saleType === "LONG_TERM" ? "Sale dài hạn" : "Flash Sale"} · {product.categoryName || "Sản phẩm"}</small><h3>{product.name}</h3><strong>{formatPrice(product.salePrice)}</strong><del className="shop-product-original-price">{formatPrice(product.originalPrice)}</del><div><span><StarRoundedIcon /> 4.8</span><button onClick={() => addToCart(product)} aria-label={`Thêm ${product.name} vào giỏ`}><AddShoppingCartOutlinedIcon /></button></div></div></article>)}</div>{saleProducts.length === 0 && <div className="shop-empty">Chương trình giảm giá mới sẽ sớm được cập nhật.</div>}</section>
      <section className="shop-recommendations"><div className="shop-section-head"><div><small>Khám phá từ nhiều danh mục</small><h2>Gợi ý dành cho bạn</h2></div><button onClick={() => navigate("/shop/categories")}>Xem tất cả →</button></div><div className="shop-product-grid">{recommendedProducts.map((product) => <article className="shop-product-card" key={`recommended-${product.id}`}><div className="shop-product-media"><button type="button" className={wishlist.some((item) => item.productId === product.id) ? "is-wishlisted" : ""} disabled={wishlistLoading || wishlistPendingIds.has(product.id)} onClick={(event) => changeWishlist(event, product)} aria-label="Thêm hoặc xoá yêu thích"><FavoriteBorderOutlinedIcon /></button><ProductImage product={product} /></div><div className="shop-product-body"><small>{product.categoryName || "Sản phẩm"}</small><h3>{product.name}</h3><strong>{formatPrice(product.price)}</strong><div><span><StarRoundedIcon /> 4.8</span><button onClick={() => addToCart(product)} aria-label={`Thêm ${product.name} vào giỏ`}><AddShoppingCartOutlinedIcon /></button></div></div></article>)}</div>{recommendedProducts.length === 0 && <div className="shop-empty">Chưa có sản phẩm còn hàng để gợi ý.</div>}</section>
      <section className="shop-trust"><div><ShoppingBagOutlinedIcon /><p><b>Thanh toán an toàn</b>Bảo mật 100%</p></div><div><LocalOfferOutlinedIcon /><p><b>Đổi trả dễ dàng</b>Trong vòng 30 ngày</p></div><div><NotificationsNoneOutlinedIcon /><p><b>Hỗ trợ 24/7</b>Luôn sẵn sàng</p></div></section>
      <ShopFlashSale products={products} deals={flashDeals} upcomingDeals={upcomingFlashDeals} notifiedIds={flashNotificationIds} onNotify={notifyFlashSale} onOpen={(product) => navigate(productUrl(product))} />
    </main>
    <aside className="shop-cart"><div className="shop-cart-card"><div className="shop-cart-head"><h3>Giỏ hàng ({cart.reduce((sum, item) => sum + item.quantity, 0)})</h3><CloseOutlinedIcon /></div>{cart.length === 0 ? <div className="shop-cart-empty"><ShoppingBagOutlinedIcon /><p>Giỏ hàng đang trống.</p><small>Thêm sản phẩm để xem tổng tiền.</small></div> : <><div>{cart.map((item) => <div className="shop-cart-line" key={item.id}><div>{imageUrl(item) ? <img src={imageUrl(item)} alt="" /> : <ShoppingBagOutlinedIcon />}</div><section><b>{item.productName}</b><small>{formatPrice(item.price)}</small><p><button onClick={() => updateQuantity(item, -1)}><RemoveOutlinedIcon /></button>{item.quantity}<button onClick={() => updateQuantity(item, 1)}><AddShoppingCartOutlinedIcon /></button></p></section><button onClick={() => removeFromCart(item.id)}><DeleteOutlineOutlinedIcon /></button></div>)}</div><div className="shop-cart-summary"><p><span>Tạm tính</span><b>{formatPrice(total)}</b></p><p><span>Vận chuyển</span><b>Miễn phí</b></p><p className="total"><span>Tổng cộng</span><b>{formatPrice(total)}</b></p><button onClick={() => navigate("/cart")}>Thanh toán ({cart.reduce((sum, item) => sum + item.quantity, 0)})</button></div></>}</div></aside>
  </div></>;
}
