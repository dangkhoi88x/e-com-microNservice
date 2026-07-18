import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import AddShoppingCartOutlinedIcon from "@mui/icons-material/AddShoppingCartOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import RemoveOutlinedIcon from "@mui/icons-material/RemoveOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { getProductById, getProducts } from "../services/productService";
import { getWishlist, toggleWishlist } from "../services/wishlistService";
import "./ShopProductDetail.css";

const money = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
const firstImage = (product) => product?.images?.find((image) => image.isPrimary)?.url || product?.images?.[0]?.url;
const slugify = (value) => (value || "san-pham").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const productUrl = (item) => `/shop/products/${slugify(item.name)}-${item.id}`;
const productIdFromSlug = (slug) => slug?.match(/[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i)?.[0] || slug;

const saveToCart = (item, quantity, variant) => {
  const cartItem = {
    ...item,
    id: variant?.id ? `${item.id}:${variant.id}` : item.id,
    productId: item.id,
    price: variant?.price ?? item.price,
    imageUrl: variant?.imageUrl || firstImage(item),
    quantity,
    selectedVariant: variant?.attributes || null,
  };
  try {
    const current = JSON.parse(sessionStorage.getItem("nova-shop-cart") || "[]");
    const cart = Array.isArray(current) ? current : [];
    const found = cart.find((entry) => entry.id === cartItem.id);
    const next = found
      ? cart.map((entry) => entry.id === cartItem.id ? { ...entry, quantity: entry.quantity + quantity } : entry)
      : [...cart, cartItem];
    sessionStorage.setItem("nova-shop-cart", JSON.stringify(next));
  } catch {
    sessionStorage.setItem("nova-shop-cart", JSON.stringify([cartItem]));
  }
};

export default function ShopProductDetail() {
  const { slug } = useParams();
  const id = productIdFromSlug(slug);
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [related, setRelated] = useState([]);
  const [selected, setSelected] = useState({});
  const [quantity, setQuantity] = useState(1);
  const [activeImage, setActiveImage] = useState("");
  const [cartNotice, setCartNotice] = useState("");
  const [wishlist, setWishlist] = useState(getWishlist);

  useEffect(() => {
    getProductById(id).then((data) => {
      setProduct(data);
      setActiveImage(firstImage(data) || "");
    }).catch(() => setProduct(null));
  }, [id]);

  useEffect(() => {
    if (product?.categoryId) getProducts({ page: 1, size: 8, categoryId: product.categoryId })
      .then((data) => setRelated((data.content || []).filter((item) => item.id !== product.id).slice(0, 4)))
      .catch(() => {});
  }, [product?.categoryId, product?.id]);
  useEffect(() => {
    const handleWishlist = (event) => {
      if (!event.target.closest(".detail-wishlist") || !product) return;
      event.preventDefault();
      const next = toggleWishlist(product);
      setWishlist(next);
    };
    document.addEventListener("click", handleWishlist);
    return () => document.removeEventListener("click", handleWishlist);
  }, [product]);
  useEffect(() => {
    document.querySelector(".detail-wishlist")?.classList.toggle("is-wishlisted", wishlist.some((item) => item.id === product?.id));
  }, [product?.id, wishlist]);

  const variant = useMemo(() => (product?.variants || []).find((item) => Object.entries(selected).every(([key, value]) => item.attributes?.[key] === value)), [product, selected]);
  if (!product) return <main className="detail-loading">Đang tải sản phẩm…</main>;

  const images = [...(product.images || []), ...(variant?.imageUrl ? [{ url: variant.imageUrl }] : [])]
    .filter((image, index, list) => image?.url && list.findIndex((item) => item.url === image.url) === index);
  const price = variant?.price ?? product.price;
  const stock = variant?.quantity ?? product.quantity ?? 0;
  const add = () => {
    saveToCart(product, quantity, variant);
    setCartNotice(`Đã thêm ${quantity} sản phẩm vào giỏ hàng.`);
  };
  const buyNow = () => {
    saveToCart(product, quantity, variant);
    navigate("/checkout");
  };

  return <main className="product-detail-page">
    <header className="detail-top"><Link to="/shop"><ShoppingBagOutlinedIcon /> NovaShop</Link><button onClick={() => navigate(-1)}><ArrowBackOutlinedIcon /> Quay lại</button></header>
    <div className="detail-breadcrumb">Shop / {product.categoryName || "Sản phẩm"} / <b>{product.name}</b></div>
    <section className="detail-layout">
      <aside className="detail-options">{(product.options || []).map((option) => <section key={option.name}><h3>{option.displayName}</h3><div className={`detail-option-values ${option.displayType === "COLOR_SWATCH" ? "colors" : ""}`}>{option.values.filter((value) => value.active !== false).map((value) => <button key={value.value} className={selected[option.name] === value.value ? "selected" : ""} onClick={() => setSelected((current) => ({ ...current, [option.name]: value.value }))}>{option.displayType === "COLOR_SWATCH" ? <span style={{ background: value.colorHex || "#a9d6e8" }} /> : value.displayValue}</button>)}</div></section>)}</aside>
      <section className="detail-gallery"><div className="detail-main-image">{activeImage ? <img src={activeImage} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><div className="detail-thumbnails">{images.map((image) => <button key={image.url} className={activeImage === image.url ? "active" : ""} onClick={() => setActiveImage(image.url)}><img src={image.url} alt="" /></button>)}</div></section>
      <section className="detail-info"><p>{product.categoryName || "Sản phẩm"}</p><h1>{product.name}</h1><div className="detail-rating"><StarRoundedIcon /> 4.8 <span>(124 đánh giá)</span></div><div className="detail-description">{product.description || "Sản phẩm được tuyển chọn với thiết kế hiện đại và chất lượng cao."}</div><div className="detail-selected">{Object.keys(selected).length ? `Đã chọn: ${Object.entries(selected).map(([key, value]) => `${key}: ${value}`).join(" · ")}` : "Chọn phiên bản phù hợp với bạn"}</div><strong className="detail-price">{money(price)}</strong><small className={stock > 0 ? "detail-stock" : "detail-stock out"}>{stock > 0 ? `Còn ${stock} sản phẩm` : "Tạm hết hàng"}</small><div className="detail-actions"><div className="detail-quantity"><button onClick={() => setQuantity((value) => Math.max(1, value - 1))}><RemoveOutlinedIcon /></button><b>{quantity}</b><button onClick={() => setQuantity((value) => Math.min(stock || 1, value + 1))}>+</button></div><button className="detail-add" disabled={!stock} onClick={add}><AddShoppingCartOutlinedIcon /> Thêm giỏ</button><button className="detail-buy" disabled={!stock} onClick={buyNow}>Mua ngay</button></div>{cartNotice && <p className="detail-cart-notice">{cartNotice}</p>}<small className="detail-delivery"><LocalShippingOutlinedIcon /> Miễn phí vận chuyển cho đơn đủ điều kiện</small><button className="detail-wishlist"><FavoriteBorderOutlinedIcon /> Thêm yêu thích</button></section>
    </section>
    <section className="detail-related"><h2>Sản phẩm liên quan</h2><div>{related.map((item) => <article key={item.id} onClick={() => navigate(productUrl(item))}><span>{firstImage(item) ? <img src={firstImage(item)} alt={item.name} /> : <ShoppingBagOutlinedIcon />}</span><h3>{item.name}</h3><strong>{money(item.price)}</strong><button>Xem chi tiết</button></article>)}</div></section>
  </main>;
}
