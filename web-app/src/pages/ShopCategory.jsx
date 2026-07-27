import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ChevronDown,
  Filter,
  Heart,
  Search,
  ShoppingBag,
  SlidersHorizontal,
  Star,
  Truck,
} from "lucide-react";
import { getCategories } from "../services/categoryService";
import useShopProductListing from "../hooks/useShopProductListing";
import "./ShopCategory.css";
import "./ShopProductListing.css";

const slugify = (value) => (value || "danh-muc")
  .toLocaleLowerCase("vi-VN")
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .replace(/đ/g, "d")
  .replace(/[^a-z0-9]+/g, "-")
  .replace(/^-|-$/g, "");

const idFromSlug = (slug) =>
  slug?.match(/[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i)?.[0] || slug;
const categoryUrl = (item) => `/shop/categories/${slugify(item.name)}-${item.id}`;
const productUrl = (item) => `/shop/products/${slugify(item.name)}-${item.productId || item.id}`;
const money = (value) => new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
}).format(value || 0);
const image = (item) => item.thumbnailUrl
  || item.images?.find((entry) => entry.isPrimary)?.url
  || item.images?.[0]?.url;

export default function ShopCategory() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const categoryId = idFromSlug(slug);
  const [categories, setCategories] = useState([]);
  const {
    aggregation: facets,
    filters,
    loading,
    pageInfo,
    products,
    query,
    resetFilters,
    setFilter,
    setQuery,
    urlParams,
  } = useShopProductListing({ categoryId, size: 24 });

  useEffect(() => {
    getCategories().then(setCategories).catch(() => {});
  }, []);

  const category = categories.find((item) => item.id === categoryId);
  const changeQuery = (value) => {
    setQuery(value);
    setFilter("q", value);
  };

  return (
    <main className="category-page">
      <section className="category-hero">
        <div>
          <p>KHÁM PHÁ BỘ SƯU TẬP</p>
          <h1>{category?.name || "Tất cả sản phẩm"}</h1>
          <span>{pageInfo.totalElements} sản phẩm phù hợp với bộ lọc của bạn</span>
        </div>
        <ShoppingBag size={88} />
      </section>

      <section className="category-shell">
        <aside className="category-sidebar">
          <div className="category-sidebar-title"><h2>Danh mục</h2><ChevronDown size={17} /></div>
          <nav>
            <Link className={!categoryId ? "active" : ""} to="/shop/categories">
              Tất cả sản phẩm
            </Link>
            {categories.map((item) => (
              <Link
                key={item.id}
                className={item.id === categoryId ? "active" : ""}
                to={categoryUrl(item)}
              >
                {item.name}
              </Link>
            ))}
          </nav>

          <div className="category-filter-title"><Filter size={17} /><h2>Bộ lọc</h2></div>
          <label className="category-search">
            <Search size={15} />
            <input placeholder="Tên sản phẩm" value={query} onChange={(event) => changeQuery(event.target.value)} />
          </label>

          <div className="category-filter">
            <b>Khoảng giá</b>
            <div>
              <input placeholder="Từ" value={urlParams.get("minPrice") || ""} onChange={(event) => setFilter("minPrice", event.target.value)} type="number" />
              <i>—</i>
              <input placeholder="Đến" value={urlParams.get("maxPrice") || ""} onChange={(event) => setFilter("maxPrice", event.target.value)} type="number" />
            </div>
            {facets.priceStats?.min != null && <small>{money(facets.priceStats.min)} – {money(facets.priceStats.max)}</small>}
          </div>

          <div className="category-filter">
            <b>Tình trạng</b>
            <label>
              <input type="checkbox" checked={filters.inStock === true} onChange={(event) => setFilter("inStock", event.target.checked ? "true" : "")} />
              {" "}Chỉ hiện sản phẩm còn hàng
            </label>
          </div>

          <div className="category-filter">
            <b>Đánh giá</b>
            {[5, 4, 3, 2, 1].map((rating) => (
              <label key={rating}>
                <input type="radio" checked={Number(filters.minRating || 0) === rating} onChange={() => setFilter("minRating", rating)} />
                {" "}Từ {rating} sao
              </label>
            ))}
            <button type="button" onClick={() => setFilter("minRating", "")}>Tất cả đánh giá</button>
          </div>
          <button className="category-reset" onClick={resetFilters}>Xóa bộ lọc</button>
        </aside>

        <div className="category-results">
          <div className="category-results-head">
            <div>
              <p>HIỂN THỊ {pageInfo.totalElements} SẢN PHẨM</p>
              <h2>{category?.name || "Tất cả sản phẩm"}</h2>
            </div>
            <label className="category-sort">
              <SlidersHorizontal size={16} />
              <select value={filters.sort} onChange={(event) => setFilter("sort", event.target.value)}>
                <option value="createdAt,desc">Mới nhất</option>
                <option value="price,asc">Giá thấp đến cao</option>
                <option value="price,desc">Giá cao đến thấp</option>
                <option value="averageRating,desc">Đánh giá cao nhất</option>
                <option value="reviewCount,desc">Nhiều đánh giá nhất</option>
              </select>
            </label>
          </div>

          <div className="category-grid">
            {products.map((item) => (
              <article key={item.productId} onClick={() => navigate(productUrl(item))}>
                <div className="category-product-image">
                  <button onClick={(event) => event.stopPropagation()} aria-label="Thêm vào yêu thích"><Heart size={17} /></button>
                  {image(item) ? <img src={image(item)} alt={item.name} /> : <ShoppingBag size={35} />}
                </div>
                <div>
                  <p>{item.categoryName || category?.name}</p>
                  <h3>{item.name}</h3>
                  <strong>{money(item.price)}</strong>
                  <span><Star size={13} fill="currentColor" /> {Number(item.averageRating || 0).toFixed(1)} · {item.reviewCount || 0} đánh giá</span>
                </div>
              </article>
            ))}
          </div>

          {loading && <div className="category-empty">Đang áp dụng bộ lọc…</div>}
          {!loading && !products.length && (
            <div className="category-empty">
              <ShoppingBag size={38} />
              <h3>Chưa có sản phẩm phù hợp</h3>
              <p>Hãy thử xóa bộ lọc hoặc chọn danh mục khác.</p>
              <button onClick={resetFilters}>Xem lại sản phẩm</button>
            </div>
          )}

          {pageInfo.totalPages > 1 && (
            <nav className="shop-listing-pagination" aria-label="Phân trang sản phẩm">
              <button disabled={pageInfo.currentPage <= 1} onClick={() => setFilter("page", pageInfo.currentPage - 1)}>← Trước</button>
              <span>Trang {pageInfo.currentPage} / {pageInfo.totalPages}</span>
              <button disabled={pageInfo.currentPage >= pageInfo.totalPages} onClick={() => setFilter("page", pageInfo.currentPage + 1)}>Sau →</button>
            </nav>
          )}

          <div className="category-services">
            <div><Truck size={22} /><p><b>Giao hàng nhanh</b>Đến tận tay trong 2–5 ngày</p></div>
            <div><Heart size={21} /><p><b>Chọn lọc kỹ lưỡng</b>Chất lượng là ưu tiên hàng đầu</p></div>
          </div>
        </div>
      </section>
    </main>
  );
}
