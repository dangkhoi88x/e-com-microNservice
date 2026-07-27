import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Filter, Search, SlidersHorizontal, Star } from "lucide-react";
import { getCategories } from "../services/categoryService";
import useShopProductListing from "../hooks/useShopProductListing";
import "./ShopSearch.css";
import "./ShopProductListing.css";

const money = (value) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value || 0);
const slugify = (value) =>
  (value || "san-pham")
    .toLocaleLowerCase("vi-VN")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
const image = (item) =>
  item.thumbnailUrl ||
  item.images?.find((entry) => entry.isPrimary)?.url ||
  item.images?.[0]?.url;

export default function ShopSearch() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const {
    aggregation, chooseSuggestion, filters, loading, pageInfo, products, query,
    resetFilters, setFilter, setQuery, submitQuery, suggestions, urlParams,
  } = useShopProductListing({ size: 24 });
  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(() => {});
  }, []);
  const submit = (event) => {
    event.preventDefault();
    submitQuery();
  };
  return (
    <main className="shop-search-page">
      <header>
        <Link to="/shop">
          Nova<span>Shop</span>
        </Link>
        <form onSubmit={submit}>
          <Search size={18} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Tìm sản phẩm, thương hiệu..."
          />
          {suggestions.length > 0 && (
            <div className="shop-search-suggestions">
              {suggestions.map((item) => (
                <button
                  type="button"
                  key={item.productId}
                  onClick={() => {
                    chooseSuggestion(item);
                  }}
                >
                  <img src={image(item)} alt="" />
                  <span>
                    {item.name}
                    <small>{money(item.price)}</small>
                  </span>
                </button>
              ))}
            </div>
          )}
        </form>
        <Link to="/cart">Giỏ hàng</Link>
      </header>
      <section className="shop-search-shell">
        <aside>
          <div className="shop-search-filter-title">
            <Filter size={17} />
            <h2>Bộ lọc</h2>
          </div>
          <b>Danh mục</b>
          {categories.map((category) => (
            <label key={category.id}>
              <input
                type="radio"
                checked={filters.categoryId === category.id}
                onChange={() => setFilter("categoryId", category.id)}
              />{" "}
              {category.name}
            </label>
          ))}
          <button onClick={() => setFilter("categoryId", "")}>
            Tất cả danh mục
          </button>
          <b>Khoảng giá</b>
          <div className="shop-search-prices">
            <input
              value={urlParams.get("minPrice") || ""}
              onChange={(event) => setFilter("minPrice", event.target.value)}
              placeholder="Từ"
              type="number"
            />
            <input
              value={urlParams.get("maxPrice") || ""}
              onChange={(event) => setFilter("maxPrice", event.target.value)}
              placeholder="Đến"
              type="number"
            />
          </div>
          <b>Tình trạng</b>
          <label>
            <input
              type="checkbox"
              checked={filters.inStock === true}
              onChange={(event) =>
                setFilter("inStock", event.target.checked ? "true" : "")
              }
            />{" "}
            Còn hàng
          </label>
          <b>Đánh giá</b>
          {[5, 4, 3, 2, 1].map((rating) => (
            <label key={rating}>
              <input
                type="radio"
                checked={Number(filters.minRating || 0) === rating}
                onChange={() => setFilter("minRating", rating)}
              />{" "}
              Từ {rating} sao
            </label>
          ))}
          <button onClick={() => setFilter("minRating", "")}>Tất cả đánh giá</button>
          <button
            className="shop-search-reset"
            onClick={() => {
              resetFilters();
            }}
          >
            Xóa bộ lọc
          </button>
        </aside>
        <section>
          <div className="shop-search-head">
            <div>
              <p>TÌM KIẾM SẢN PHẨM</p>
              <h1>
                {filters.q ? `Kết quả cho “${filters.q}”` : "Khám phá sản phẩm"}
              </h1>
              <small>{pageInfo.totalElements} sản phẩm phù hợp</small>
            </div>
            <label>
              <SlidersHorizontal size={16} />
              <select
                value={filters.sort}
                onChange={(event) => setFilter("sort", event.target.value)}
              >
                <option value="createdAt,desc">Mới nhất</option>
                <option value="price,asc">Giá thấp đến cao</option>
                <option value="price,desc">Giá cao đến thấp</option>
                <option value="averageRating,desc">Đánh giá cao nhất</option>
                <option value="reviewCount,desc">Nhiều đánh giá nhất</option>
              </select>
            </label>
          </div>
          <div className="shop-search-chips">
            {aggregation.categories?.slice(0, 6).map((item) => (
              <span key={item.name}>
                {item.name} · {item.count}
              </span>
            ))}
          </div>
          {loading ? (
            <div className="shop-search-empty">Đang tìm sản phẩm…</div>
          ) : (
            <div className="shop-search-grid">
              {products.map((item) => (
                <article
                  key={item.productId}
                  onClick={() =>
                    navigate(
                      `/shop/products/${slugify(item.name)}-${item.productId}`,
                    )
                  }
                >
                  {image(item) ? (
                    <img src={image(item)} alt={item.name} />
                  ) : (
                    <Search size={38} />
                  )}
                  <div>
                    <small>{item.categoryName}</small>
                    <h2>{item.name}</h2>
                    <p>
                      <Star size={13} fill="currentColor" /> {Number(item.averageRating || 0).toFixed(1)}
                      <small> ({item.reviewCount || 0})</small>
                    </p>
                    <strong>{money(item.price)}</strong>
                  </div>
                </article>
              ))}
            </div>
          )}
          {!loading && !products.length && (
            <div className="shop-search-empty">
              Không tìm thấy sản phẩm phù hợp. Hãy thay đổi từ khóa hoặc bộ lọc.
            </div>
          )}
          {pageInfo.totalPages > 1 && (
            <nav className="shop-listing-pagination" aria-label="Phân trang sản phẩm">
              <button disabled={pageInfo.currentPage <= 1} onClick={() => setFilter("page", pageInfo.currentPage - 1)}>← Trước</button>
              <span>Trang {pageInfo.currentPage} / {pageInfo.totalPages}</span>
              <button disabled={pageInfo.currentPage >= pageInfo.totalPages} onClick={() => setFilter("page", pageInfo.currentPage + 1)}>Sau →</button>
            </nav>
          )}
        </section>
      </section>
    </main>
  );
}
