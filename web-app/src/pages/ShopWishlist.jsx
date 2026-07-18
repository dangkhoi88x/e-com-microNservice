import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft, Heart, ShoppingBag, Trash2 } from "lucide-react";
import { getWishlist, loadWishlist, removeWishlistItem } from "../services/wishlistService";
import "./ShopWishlist.css";

const money = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value || 0);
const slugify = (value) => (value || "san-pham").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const image = (item) => item.imageUrl || item.images?.find((entry) => entry.isPrimary)?.url || item.images?.[0]?.url || "https://placehold.co/300x300/e7f2f8/3b82c4?text=Nova";

export default function ShopWishlist() {
  const navigate = useNavigate();
  const [items, setItems] = useState(getWishlist);
  useEffect(() => { loadWishlist().then(setItems).catch(() => {}); }, []);
  const remove = (item) => setItems(removeWishlistItem(item));
  const addToCart = (product) => { try { const cart = JSON.parse(sessionStorage.getItem("nova-shop-cart") || "[]"); const found = cart.find((item) => item.id === product.id); const next = found ? cart.map((item) => item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item) : [...cart, { ...product, quantity: 1 }]; sessionStorage.setItem("nova-shop-cart", JSON.stringify(next)); navigate("/cart"); } catch { sessionStorage.setItem("nova-shop-cart", JSON.stringify([{ ...product, quantity: 1 }])); navigate("/cart"); } };
  return <main className="wishlist-page"><header><Link className="wishlist-brand" to="/shop"><i>N</i>Nova<span>Shop</span></Link><Link to="/shop"><ArrowLeft size={17}/> Tiếp tục mua sắm</Link></header><section className="wishlist-shell"><p>WATCHLIST</p><h1>Sản phẩm yêu thích</h1><span>Những món bạn đang quan tâm, sẵn sàng để mua bất cứ lúc nào.</span>{!items.length ? <div className="wishlist-empty"><Heart size={45}/><h2>Wishlist của bạn đang trống</h2><p>Nhấn biểu tượng trái tim ở sản phẩm để lưu lại cho lần mua sau.</p><Link to="/shop">Khám phá sản phẩm</Link></div> : <><div className="wishlist-count"><Heart size={16} fill="currentColor"/>{items.length} sản phẩm đã lưu</div><div className="wishlist-grid">{items.map((item) => <article key={item.id}><button className="wishlist-remove" onClick={() => remove(item)} aria-label="Xoá khỏi yêu thích"><Trash2 size={16}/></button><div className="wishlist-image" onClick={() => navigate(`/shop/products/${slugify(item.name)}-${item.productId || item.id}`)}><img src={image(item)} alt={item.name}/></div><div className="wishlist-info"><p>{item.categoryName || "NovaShop selection"}</p><h2>{item.name}</h2><strong>{money(item.price)}</strong><button onClick={() => addToCart(item)}><ShoppingBag size={16}/> Thêm vào giỏ</button></div></article>)}</div></>}</section></main>;
}
