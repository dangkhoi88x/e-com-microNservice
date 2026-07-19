import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import CheckroomOutlinedIcon from "@mui/icons-material/CheckroomOutlined";
import DevicesOtherOutlinedIcon from "@mui/icons-material/DevicesOtherOutlined";
import KitchenOutlinedIcon from "@mui/icons-material/KitchenOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { getCategories } from "../services/categoryService";
import { getProducts } from "../services/productService";
import "./ShopHotDeals.css";
import "./ShopHotDealsCategoryTabs.css";
import "./ShopHotDealsSections.css";

const price = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
const slugify = (value) => (value || "san-pham").toLocaleLowerCase("vi-VN").normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const image = (product) => product?.images?.find((item) => item.isPrimary)?.url || product?.images?.[0]?.url;
const icons = [CheckroomOutlinedIcon, DevicesOtherOutlinedIcon, KitchenOutlinedIcon, ShoppingBagOutlinedIcon];

export default function ShopHotDeals() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [visible, setVisible] = useState(8);
  const [selectedCategory, setSelectedCategory] = useState(null);
  useEffect(() => { Promise.all([getProducts({ page: 1, size: 24 }), getCategories()]).then(([productData, categoryData]) => { setProducts(productData.content || []); setCategories(categoryData || []); }).catch(() => {}); }, []);
  const filteredProducts = useMemo(() => selectedCategory ? products.filter((product) => product.categoryId === selectedCategory.id) : products, [products, selectedCategory]);
  const deals = useMemo(() => filteredProducts.slice(0, visible), [filteredProducts, visible]);
  const chooseCategory = (category) => { setSelectedCategory(null); window.setTimeout(() => document.getElementById(`hot-deals-category-${category.id}`)?.scrollIntoView({ behavior: "smooth", block: "start" }), 0); };
  return <main className="hot-deals-page">
    <section className="hot-deals-hero"><div className="hot-deals-shine" /><span className="hot-deals-pill"><LocalOfferOutlinedIcon /> TOP DEAL</span><div className="hot-deals-orb left">✦</div><div className="hot-deals-orb right">%</div><p>ƯU ĐÃI ĐỘC QUYỀN</p><h1>CAM KẾT SIÊU RẺ</h1><div className="hot-deals-benefits"><b>Giảm đến <strong>50%</strong></b><b>Coupon đến <strong>200K</strong></b><b>Giao nhanh <strong>2H</strong></b></div></section>
    <section className="hot-deals-categories"><h2>Khám phá theo danh mục</h2><div>{categories.slice(0, 10).map((category, index) => { const Icon = icons[index % icons.length]; return <button key={category.id} className={selectedCategory?.id === category.id ? "active" : ""} onClick={() => chooseCategory(category)}><span><Icon /></span><b>{category.name}</b></button>; })}</div></section>
    <section className="hot-deals-content" id="hot-deals-products"><div className="hot-deals-title"><span><LocalOfferOutlinedIcon /> HOT DEAL MỖI NGÀY</span><h2>{selectedCategory ? selectedCategory.name : "Săn deal nhanh · Giá tốt mỗi ngày"}</h2><p>{selectedCategory ? `Ưu đãi đang có trong danh mục ${selectedCategory.name}.` : "Danh sách ưu đãi được chọn riêng cho bạn."}</p></div><nav className="hot-deals-tabs"><button className={!selectedCategory ? "active" : ""} onClick={() => { setSelectedCategory(null); setVisible(8); }}>Tất cả</button>{categories.slice(0, 6).map((category) => <button key={category.id} className={selectedCategory?.id === category.id ? "active" : ""} onClick={() => chooseCategory(category)}>{category.name}</button>)}</nav><div className="hot-deals-grid">{deals.map((product, index) => <article key={product.id} onClick={() => navigate(`/shop/products/${slugify(product.name)}-${product.id}`)}><div className="hot-deals-image">{index < 5 && <i>−{15 + index * 5}%</i>}{image(product) ? <img src={image(product)} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><div className="hot-deals-card-body"><small><LocalOfferOutlinedIcon /> TOP DEAL</small><h3>{product.name}</h3><p><StarRoundedIcon /> 4.8 <span>· Đã bán {120 + index * 81}</span></p><strong>{price(product.price)}</strong><del>{price(Number(product.price || 0) * 1.2)}</del></div></article>)}</div>{!deals.length && <p className="hot-deals-empty">Danh mục này chưa có sản phẩm ưu đãi.</p>}{filteredProducts.length > visible && <button className="hot-deals-more" onClick={() => setVisible((current) => Math.min(current + 8, filteredProducts.length))}>Xem thêm ưu đãi</button>}</section>
    <section className="hot-deals-category-sections">{categories.map((category) => { const items = products.filter((product) => product.categoryId === category.id); if (!items.length) return null; return <section id={`hot-deals-category-${category.id}`} key={category.id} className="hot-deals-category-section"><header><span><LocalOfferOutlinedIcon /> DEAL THEO DANH MỤC</span><h2>{category.name}</h2><button onClick={() => navigate(`/shop/categories/${slugify(category.name)}-${category.id}`)}>Xem tất cả</button></header><div className="hot-deals-grid">{items.slice(0, 8).map((product, index) => <article key={product.id} onClick={() => navigate(`/shop/products/${slugify(product.name)}-${product.id}`)}><div className="hot-deals-image">{index < 4 && <i>−{15 + index * 5}%</i>}{image(product) ? <img src={image(product)} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div><div className="hot-deals-card-body"><small><LocalOfferOutlinedIcon /> TOP DEAL</small><h3>{product.name}</h3><p><StarRoundedIcon /> 4.8 <span>· Đã bán {120 + index * 81}</span></p><strong>{price(product.price)}</strong><del>{price(Number(product.price || 0) * 1.2)}</del></div></article>)}</div></section>; })}</section>
  </main>;
}
