import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AddShoppingCartOutlinedIcon from "@mui/icons-material/AddShoppingCartOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import { searchProductSuggestions } from "../services/productService";
import "./ShopStoreHeader.css";

const image = (product) => product?.thumbnailUrl || product?.images?.find((item) => item.isPrimary)?.url || product?.images?.[0]?.url;
const slugify = (value) => (value || "san-pham").toLocaleLowerCase("vi-VN").normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const url = (product) => `/shop/products/${slugify(product.name)}-${product.productId || product.id}`;
const cartQuantity = () => { try { return JSON.parse(sessionStorage.getItem("nova-shop-cart") || "[]").reduce((total, item) => total + Number(item.quantity || 0), 0); } catch { return 0; } };

export default function ShopStoreHeader({ showBack = false }) {
  const navigate = useNavigate(); const [query, setQuery] = useState(""); const [suggestions, setSuggestions] = useState([]); const [cartCount, setCartCount] = useState(cartQuantity);
  useEffect(() => { const refreshCart = () => setCartCount(cartQuantity()); window.addEventListener("focus", refreshCart); return () => window.removeEventListener("focus", refreshCart); }, []);
  useEffect(() => { const timer = window.setTimeout(() => searchProductSuggestions(query).then(setSuggestions).catch(() => setSuggestions([])), 300); return () => window.clearTimeout(timer); }, [query]);
  return <header className="store-header"><Link className="store-header-brand" to="/shop"><ShoppingBagOutlinedIcon /> NovaShop</Link><div className="store-header-search-wrap"><label className="store-header-search"><SearchOutlinedIcon /><input value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") navigate(`/shop/search?q=${encodeURIComponent(query)}`); }} placeholder="Tìm sản phẩm khác..." aria-label="Tìm sản phẩm" /></label>{suggestions.length > 0 && <div className="store-header-results">{suggestions.map((product) => <Link key={product.productId || product.id} to={url(product)} onClick={() => setQuery("")}><img src={image(product)} alt="" /><span>{product.name}</span></Link>)}</div>}</div><div className="store-header-actions"><Link className="store-header-cart" to="/cart"><AddShoppingCartOutlinedIcon /> Giỏ hàng {cartCount > 0 && <b>{cartCount}</b>}</Link>{showBack && <button onClick={() => navigate(-1)}><ArrowBackOutlinedIcon /> Quay lại</button>}</div></header>;
}
