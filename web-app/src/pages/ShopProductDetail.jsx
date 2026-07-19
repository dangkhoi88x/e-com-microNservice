import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import AddShoppingCartOutlinedIcon from "@mui/icons-material/AddShoppingCartOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import FavoriteIcon from "@mui/icons-material/Favorite";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import RemoveOutlinedIcon from "@mui/icons-material/RemoveOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { getProductById, getProducts } from "../services/productService";
import { getWishlist, toggleWishlistTransaction } from "../services/wishlistService";
import "./ShopProductDetail.css";
import "./ShopProductDetailDense.css";

const money = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
const firstImage = (product) => product?.images?.find((image) => image.isPrimary)?.url || product?.images?.[0]?.url;
const slugify = (value) => (value || "san-pham").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const productUrl = (item) => `/shop/products/${slugify(item.name)}-${item.id}`;
const productIdFromSlug = (slug) => slug?.match(/[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i)?.[0] || slug;
const getCartCount = () => {
  try { return JSON.parse(sessionStorage.getItem("nova-shop-cart") || "[]").reduce((total, item) => total + Number(item.quantity || 0), 0); } catch { return 0; }
};

const saveToCart = (item, quantity, variant) => {
  const cartItem = { ...item, id: variant?.id ? `${item.id}:${variant.id}` : item.id, productId: item.id, price: variant?.price ?? item.price, imageUrl: variant?.imageUrl || firstImage(item), quantity, selectedVariant: variant?.attributes || null };
  try {
    const current = JSON.parse(sessionStorage.getItem("nova-shop-cart") || "[]");
    const cart = Array.isArray(current) ? current : [];
    const found = cart.find((entry) => entry.id === cartItem.id);
    const next = found ? cart.map((entry) => entry.id === cartItem.id ? { ...entry, quantity: entry.quantity + quantity } : entry) : [...cart, cartItem];
    sessionStorage.setItem("nova-shop-cart", JSON.stringify(next));
  } catch { sessionStorage.setItem("nova-shop-cart", JSON.stringify([cartItem])); }
};

export default function ShopProductDetail() {
  const { slug } = useParams();
  const id = productIdFromSlug(slug);
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [related, setRelated] = useState([]);
  const [catalogProducts, setCatalogProducts] = useState([]);
  const [selected, setSelected] = useState({});
  const [quantity, setQuantity] = useState(1);
  const [activeImage, setActiveImage] = useState("");
  const [cartNotice, setCartNotice] = useState("");
  const [cartCount, setCartCount] = useState(getCartCount);
  const [wishlist, setWishlist] = useState(getWishlist);
  const [wishlistNotice, setWishlistNotice] = useState("");
  const [wishlistPending, setWishlistPending] = useState(false);
  const [productSearch, setProductSearch] = useState("");

  useEffect(() => {
    getProductById(id).then((data) => { setProduct(data); setActiveImage(firstImage(data) || ""); setSelected({}); setQuantity(1); }).catch(() => setProduct(null));
  }, [id]);
  useEffect(() => {
    getProducts({ page: 1, size: 60 }).then((data) => setCatalogProducts(data.content || [])).catch(() => {});
  }, []);
  useEffect(() => {
    if (!product?.categoryId) return;
    getProducts({ page: 1, size: 8, categoryId: product.categoryId }).then((data) => setRelated((data.content || []).filter((item) => item.id !== product.id).slice(0, 4))).catch(() => {});
  }, [product?.categoryId, product?.id]);

  const variant = useMemo(() => (product?.variants || []).find((item) => Object.entries(selected).every(([key, value]) => item.attributes?.[key] === value)), [product, selected]);
  const searchResults = useMemo(() => {
    const keyword = productSearch.trim().toLocaleLowerCase("vi-VN");
    return keyword ? catalogProducts.filter((item) => item.id !== product?.id && item.name?.toLocaleLowerCase("vi-VN").includes(keyword)).slice(0, 6) : [];
  }, [catalogProducts, product?.id, productSearch]);
  const wished = wishlist.some((item) => item.id === product?.id);
  if (!product) return <main className="detail-loading">Đang tải sản phẩm…</main>;

  const images = [...(product.images || []), ...(variant?.imageUrl ? [{ url: variant.imageUrl }] : [])].filter((image, index, list) => image?.url && list.findIndex((item) => item.url === image.url) === index);
  const price = variant?.price ?? product.price;
  const stock = variant?.quantity ?? product.quantity ?? 0;
  const add = () => { saveToCart(product, quantity, variant); setCartCount(getCartCount()); setCartNotice(`Đã thêm ${quantity} sản phẩm vào giỏ hàng.`); };
  const buyNow = () => { saveToCart(product, quantity, variant); navigate("/checkout"); };
  const changeWishlist = async () => {
    if (wishlistPending) return;
    setWishlistPending(true);
    try {
      const result = await toggleWishlistTransaction(product);
      setWishlist(result.items);
      setWishlistNotice("");
    } catch {
      setWishlist(getWishlist());
      setWishlistNotice("Không thể đồng bộ yêu thích. Trạng thái đã được hoàn tác.");
    } finally { setWishlistPending(false); }
  };

  return <main className="product-detail-page">
    <header className="detail-top detail-store-header">
      <Link to="/shop"><ShoppingBagOutlinedIcon /> NovaShop</Link>
      <div className="detail-search-wrap">
        <label className="detail-search"><SearchOutlinedIcon /><input value={productSearch} onChange={(event) => setProductSearch(event.target.value)} placeholder="Tìm sản phẩm khác..." aria-label="Tìm sản phẩm" /></label>
        {searchResults.length > 0 && <div className="detail-search-results">{searchResults.map((item) => <Link key={item.id} to={productUrl(item)} onClick={() => setProductSearch("")}><img src={firstImage(item)} alt="" /><span>{item.name}<small>{money(item.price)}</small></span></Link>)}</div>}
      </div>
      <div className="detail-header-actions"><Link className="detail-cart-link" to="/cart"><AddShoppingCartOutlinedIcon /> Giỏ hàng {cartCount > 0 && <b>{cartCount}</b>}</Link><button onClick={() => navigate(-1)}><ArrowBackOutlinedIcon /> Quay lại</button></div>
    </header>
    <nav className="detail-breadcrumb" aria-label="Breadcrumb"><Link to="/shop">Shop</Link><span>/</span>{product.categoryId ? <Link to={`/shop/categories/${slugify(product.categoryName)}-${product.categoryId}`}>{product.categoryName || "Danh mục"}</Link> : <span>{product.categoryName || "Sản phẩm"}</span>}<span>/</span><Link to={productUrl(product)}>{product.name}</Link></nav>
    <section className="detail-layout">
      <aside className="detail-options">{(product.options || []).map((option) => <section key={option.name}><h3>{option.displayName}</h3><div className={`detail-option-values ${option.displayType === "COLOR_SWATCH" ? "colors" : ""}`}>{option.values.filter((value) => value.active !== false).map((value) => <button key={value.value} className={selected[option.name] === value.value ? "selected" : ""} onClick={() => setSelected((current) => ({ ...current, [option.name]: value.value }))}>{option.displayType === "COLOR_SWATCH" ? <span style={{ background: value.colorHex || "#a9d6e8" }} /> : value.displayValue}</button>)}</div></section>)}</aside>
      <section className="detail-gallery"><div className="detail-main-image">{activeImage ? <img src={activeImage} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><div className="detail-thumbnails">{images.map((image) => <button key={image.url} className={activeImage === image.url ? "active" : ""} onClick={() => setActiveImage(image.url)}><img src={image.url} alt="" /></button>)}</div></section>
      <section className="detail-info"><p>{product.categoryName || "Sản phẩm"}</p><h1>{product.name}</h1><div className="detail-rating"><StarRoundedIcon /> 4.8 <span>(124 đánh giá)</span></div><div className="detail-description">{product.description || "Sản phẩm được tuyển chọn với thiết kế hiện đại và chất lượng cao."}</div><div className="detail-selected">{Object.keys(selected).length ? `Đã chọn: ${Object.entries(selected).map(([key, value]) => `${key}: ${value}`).join(" · ")}` : "Chọn phiên bản phù hợp với bạn"}</div><strong className="detail-price">{money(price)}</strong><small className={stock > 0 ? "detail-stock" : "detail-stock out"}>{stock > 0 ? `Còn ${stock} sản phẩm` : "Tạm hết hàng"}</small><div className="detail-actions"><div className="detail-quantity"><button onClick={() => setQuantity((value) => Math.max(1, value - 1))}><RemoveOutlinedIcon /></button><b>{quantity}</b><button onClick={() => setQuantity((value) => Math.min(stock || 1, value + 1))}>+</button></div><button className="detail-add" disabled={!stock} onClick={add}><AddShoppingCartOutlinedIcon /> Thêm giỏ</button><button className="detail-buy" disabled={!stock} onClick={buyNow}>Mua ngay</button></div>{cartNotice && <p className="detail-cart-notice">{cartNotice}</p>}<small className="detail-delivery"><LocalShippingOutlinedIcon /> Miễn phí vận chuyển cho đơn đủ điều kiện</small><button className={`detail-wishlist ${wished ? "is-wishlisted" : ""}`} disabled={wishlistPending} onClick={changeWishlist}>{wished ? <FavoriteIcon /> : <FavoriteBorderOutlinedIcon />} {wishlistPending ? "Đang lưu..." : wished ? "Đã yêu thích" : "Thêm yêu thích"}</button>{wishlistNotice && <p className="detail-wishlist-notice">{wishlistNotice}</p>}</section>
    </section>
    <section className="detail-content-sections"><article className="detail-specifications"><h2>Product Specifications</h2><dl><div><dt>Danh mục</dt><dd>{product.categoryName || "Đang cập nhật"}</dd></div><div><dt>SKU</dt><dd>{variant?.sku || product.id}</dd></div><div><dt>Tình trạng</dt><dd className={stock > 0 ? "in-stock" : "out-stock"}>{stock > 0 ? "Còn hàng" : "Hết hàng"}</dd></div><div><dt>Giá</dt><dd>{money(price)}</dd></div>{Object.entries(variant?.attributes || selected).map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value}</dd></div>)}</dl></article><article className="detail-product-description"><h2>Product Description</h2><p>{product.description || "Sản phẩm được tuyển chọn với thiết kế hiện đại, chất lượng tốt và phù hợp cho nhu cầu sử dụng hằng ngày."}</p></article></section>
    <section className="detail-related"><h2>Sản phẩm liên quan</h2><div>{related.map((item) => <article key={item.id} onClick={() => navigate(productUrl(item))}><span>{firstImage(item) ? <img src={firstImage(item)} alt={item.name} /> : <ShoppingBagOutlinedIcon />}</span><h3>{item.name}</h3><strong>{money(item.price)}</strong><button>Xem chi tiết</button></article>)}</div></section>
  </main>;
}
