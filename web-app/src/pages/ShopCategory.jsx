import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ChevronDown, Filter, Heart, Search, ShoppingBag, SlidersHorizontal, Star, Truck } from "lucide-react";
import { getCategories } from "../services/categoryService";
import { getProducts } from "../services/productService";
import "./ShopCategory.css";

const slugify = (value) => (value || "danh-muc").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const idFromSlug = (slug) => slug?.match(/[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i)?.[0] || slug;
const categoryUrl = (item) => `/shop/categories/${slugify(item.name)}-${item.id}`;
const productUrl = (item) => `/shop/products/${slugify(item.name)}-${item.id}`;
const formatPrice = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value || 0);
const imageUrl = (item) => item.images?.find((entry) => entry.isPrimary)?.url || item.images?.[0]?.url;

export default function ShopCategory() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const categoryId = idFromSlug(slug);
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState("popular");
  const [min, setMin] = useState("");
  const [max, setMax] = useState("");
  useEffect(() => { getCategories().then(setCategories).catch(() => {}); }, []);
  useEffect(() => { getProducts({ page: 1, size: 60, categoryId }).then((data) => setProducts(data.content || [])).catch(() => setProducts([])); }, [categoryId]);
  const category = categories.find((item) => item.id === categoryId);
  const shownProducts = useMemo(() => products.filter((item) => item.name?.toLowerCase().includes(query.toLowerCase()) && (!min || Number(item.price) >= Number(min)) && (!max || Number(item.price) <= Number(max))).sort((a, b) => sort === "price-low" ? a.price - b.price : sort === "price-high" ? b.price - a.price : sort === "newest" ? String(b.createdAt || "").localeCompare(String(a.createdAt || "")) : 0), [products, query, min, max, sort]);
  const reset = () => { setQuery(""); setMin(""); setMax(""); setSort("popular"); };
  return <main className="category-page">
    <header className="category-topbar"><Link to="/shop" className="category-brand"><i>N</i>Nova<span>Shop</span></Link><label><Search size={17} /><input placeholder="Tìm trong danh mục này" value={query} onChange={(event) => setQuery(event.target.value)} /></label><Link to="/cart"><ShoppingBag size={18} /> Giỏ hàng</Link></header>
    <section className="category-hero"><div><p>KHÁM PHÁ BỘ SƯU TẬP</p><h1>{category?.name || "Danh mục sản phẩm"}</h1><span>{products.length} sản phẩm được tuyển chọn dành cho bạn</span></div><ShoppingBag size={88} /></section>
    <section className="category-shell"><aside className="category-sidebar"><div className="category-sidebar-title"><h2>Danh mục</h2><ChevronDown size={17} /></div><nav>{categories.map((item) => <Link key={item.id} className={item.id === categoryId ? "active" : ""} to={categoryUrl(item)}>{item.name}</Link>)}</nav><div className="category-filter-title"><Filter size={17} /><h2>Bộ lọc</h2></div><label className="category-search"><Search size={15} /><input placeholder="Tên sản phẩm" value={query} onChange={(event) => setQuery(event.target.value)} /></label><div className="category-filter"><b>Khoảng giá</b><div><input placeholder="Từ" value={min} onChange={(event) => setMin(event.target.value)} /><i>—</i><input placeholder="Đến" value={max} onChange={(event) => setMax(event.target.value)} /></div></div><div className="category-filter"><b>Ưu đãi</b><label><input type="checkbox" /> Đang giảm giá</label><label><input type="checkbox" /> Miễn phí vận chuyển</label></div><button className="category-reset" onClick={reset}>Xoá bộ lọc</button></aside>
      <div className="category-results"><div className="category-results-head"><div><p>HIỂN THỊ {shownProducts.length} SẢN PHẨM</p><h2>{category?.name || "Sản phẩm"}</h2></div><label className="category-sort"><SlidersHorizontal size={16} /><select value={sort} onChange={(event) => setSort(event.target.value)}><option value="popular">Phổ biến nhất</option><option value="newest">Mới nhất</option><option value="price-low">Giá thấp đến cao</option><option value="price-high">Giá cao đến thấp</option></select></label></div><div className="category-grid">{shownProducts.map((item, index) => <article key={item.id} onClick={() => navigate(productUrl(item))}><div className="category-product-image">{index < 3 && <b>−{10 + index * 5}%</b>}<button onClick={(event) => event.stopPropagation()}><Heart size={17} /></button>{imageUrl(item) ? <img src={imageUrl(item)} alt={item.name} /> : <ShoppingBag size={35} />}</div><div><p>{item.categoryName || category?.name}</p><h3>{item.name}</h3><strong>{formatPrice(item.price)}</strong><span><Star size={13} fill="currentColor" /> 4.8 · Đã bán 120</span></div></article>)}</div>{!shownProducts.length && <div className="category-empty"><ShoppingBag size={38} /><h3>Chưa có sản phẩm phù hợp</h3><p>Hãy thử xoá bộ lọc hoặc chọn danh mục khác.</p><button onClick={reset}>Xem lại sản phẩm</button></div>}<div className="category-services"><div><Truck size={22} /><p><b>Giao hàng nhanh</b>Đến tận tay trong 2–5 ngày</p></div><div><Heart size={21} /><p><b>Chọn lọc kỹ lưỡng</b>Chất lượng là ưu tiên hàng đầu</p></div></div></div>
    </section>
  </main>;
}
