import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AddShoppingCartOutlinedIcon from "@mui/icons-material/AddShoppingCartOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import { getProducts } from "../services/productService";
import "./ShopStoreHeader.css";

const firstImage = (product) => product?.images?.find((image) => image.isPrimary)?.url || product?.images?.[0]?.url;
const slugify = (value) => (value || "san-pham").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const productUrl = (product) => `/shop/products/${slugify(product.name)}-${product.id}`;
const cartQuantity = () => { try { return JSON.parse(sessionStorage.getItem("nova-shop-cart") || "[]").reduce((total, item) => total + Number(item.quantity || 0), 0); } catch { return 0; } };

export default function ShopStoreHeader({ showBack = false }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [products, setProducts] = useState([]);
  const [cartCount, setCartCount] = useState(cartQuantity);
  useEffect(() => {
    getProducts({ page: 1, size: 60 }).then((data) => setProducts(data.content || [])).catch(() => {});
    const refreshCart = () => setCartCount(cartQuantity());
    window.addEventListener("focus", refreshCart);
    return () => window.removeEventListener("focus", refreshCart);
  }, []);
  const matches = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase("vi-VN");
    return keyword ? products.filter((product) => product.name?.toLocaleLowerCase("vi-VN").includes(keyword)).slice(0, 6) : [];
  }, [products, query]);
  return <header className="store-header">
    <Link className="store-header-brand" to="/shop"><ShoppingBagOutlinedIcon /> NovaShop</Link>
    <div className="store-header-search-wrap"><label className="store-header-search"><SearchOutlinedIcon /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm sản phẩm khác..." aria-label="Tìm sản phẩm" /></label>{matches.length > 0 && <div className="store-header-results">{matches.map((product) => <Link key={product.id} to={productUrl(product)} onClick={() => setQuery("")}><img src={firstImage(product)} alt="" /><span>{product.name}</span></Link>)}</div>}</div>
    <div className="store-header-actions"><Link className="store-header-cart" to="/cart"><AddShoppingCartOutlinedIcon /> Giỏ hàng {cartCount > 0 && <b>{cartCount}</b>}</Link>{showBack && <button onClick={() => navigate(-1)}><ArrowBackOutlinedIcon /> Quay lại</button>}</div>
  </header>;
}
