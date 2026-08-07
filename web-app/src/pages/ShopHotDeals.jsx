import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import CheckroomOutlinedIcon from "@mui/icons-material/CheckroomOutlined";
import DevicesOtherOutlinedIcon from "@mui/icons-material/DevicesOtherOutlined";
import KitchenOutlinedIcon from "@mui/icons-material/KitchenOutlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { getCategories } from "../services/categoryService";
import { searchProducts } from "../services/productService";
import { claimPromotion, getActivePromotions, getClaimedPromotions } from "../services/promotionService";
import { isAuthenticated } from "../services/authenticationService";
import "./ShopHotDeals.css";
import "./ShopHotDealsCategoryTabs.css";
import "./ShopHotDealsSections.css";

const price = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
const slugify = (value) => (value || "san-pham").toLocaleLowerCase("vi-VN").normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const image = (product) => product?.thumbnailUrl || product?.images?.find((item) => item.isPrimary)?.url || product?.images?.[0]?.url;
const productId = (product) => product?.productId || product?.id;
const icons = [CheckroomOutlinedIcon, DevicesOtherOutlinedIcon, KitchenOutlinedIcon, ShoppingBagOutlinedIcon];
const searchInStock = (params) => searchProducts({ page: 1, size: 8, inStock: true, sort: "createdAt,desc", ...params });
const dealMoney = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));

function DealCard({ product, index, onClick }) {
  return <article onClick={onClick}>
    <div className="hot-deals-image">{index < 5 && <i>−{15 + index * 5}%</i>}{image(product) ? <img src={image(product)} alt={product.name} /> : <ShoppingBagOutlinedIcon />}</div>
    <div className="hot-deals-card-body"><small><LocalOfferOutlinedIcon /> TOP DEAL</small><h3>{product.name}</h3><p><StarRoundedIcon /> 4.8 <span>· Đã bán {120 + index * 81}</span></p><strong>{price(product.price)}</strong><del>{price(Number(product.price || 0) * 1.2)}</del></div>
  </article>;
}

export default function ShopHotDeals() {
  const navigate = useNavigate();
  const [featured, setFeatured] = useState([]);
  const [categories, setCategories] = useState([]);
  const [categoryDeals, setCategoryDeals] = useState({});
  const [visible, setVisible] = useState(8);
  const [loading, setLoading] = useState(true);
  const [promotions, setPromotions] = useState([]);
  const [claimedIds, setClaimedIds] = useState(new Set());
  const [claimingId, setClaimingId] = useState("");
  const [promotionNotice, setPromotionNotice] = useState("");

  useEffect(() => {
    let active = true;
    Promise.allSettled([searchInStock({ size: 16 }), getCategories()])
      .then(async ([featuredResult, categoryResult]) => {
        const categoryList = categoryResult.status === "fulfilled" ? (categoryResult.value || []).slice(0, 10) : [];
        const results = await Promise.allSettled(categoryList.map((category) => searchInStock({ categoryId: category.id })));
        if (!active) return;
        setFeatured(featuredResult.status === "fulfilled" ? featuredResult.value.content || [] : []);
        setCategories(categoryList);
        setCategoryDeals(Object.fromEntries(categoryList.map((category, index) => [category.id, results[index].status === "fulfilled" ? results[index].value.content || [] : []])));
      })
      .catch(() => { if (active) { setFeatured([]); setCategories([]); setCategoryDeals({}); } })
      .finally(() => { if (active) setLoading(false); });

    Promise.all([getActivePromotions(), isAuthenticated() ? getClaimedPromotions() : Promise.resolve([])])
      .then(([promotionResult, claimedResult]) => {
        if (!active) return;
        setPromotions((promotionResult || []).filter((promotion) => promotion.status === "ACTIVE"));
        setClaimedIds(new Set((claimedResult || []).map((promotion) => promotion.id)));
      })
      .catch(() => { if (active) setPromotions([]); });
    return () => { active = false; };
  }, []);

  const deals = useMemo(() => featured.slice(0, visible), [featured, visible]);
  const openProduct = (product) => navigate(`/shop/products/${slugify(product.name)}-${productId(product)}`);
  const chooseCategory = (category) => document.getElementById(`hot-deals-category-${category.id}`)?.scrollIntoView({ behavior: "smooth", block: "start" });
  const handleClaim = async (promotion) => {
    if (!isAuthenticated()) return navigate("/shop/login?redirect=/shop/hot-deals");
    setClaimingId(promotion.id);
    setPromotionNotice("");
    try {
      await claimPromotion(promotion.id);
      setClaimedIds((current) => new Set([...current, promotion.id]));
    } catch (error) {
      setPromotionNotice(error.response?.data?.message || "Không thể claim mã khuyến mãi lúc này.");
    } finally {
      setClaimingId("");
    }
  };

  return <main className="hot-deals-page">
    <section className="hot-deals-hero"><div className="hot-deals-shine" /><span className="hot-deals-pill"><LocalOfferOutlinedIcon /> TOP DEAL</span><div className="hot-deals-orb left">✦</div><div className="hot-deals-orb right">%</div><p>ƯU ĐÃI ĐỘC QUYỀN</p><h1>CAM KẾT SIÊU RẺ</h1><div className="hot-deals-benefits"><b>Giảm đến <strong>50%</strong></b><b>Coupon đến <strong>200K</strong></b><b>Giao nhanh <strong>2H</strong></b></div></section>
    <section className="hot-deals-promotions"><div className="hot-deals-title"><span><LocalOfferOutlinedIcon /> ƯU ĐÃI DÀNH CHO BẠN</span><h2>Claim mã giảm giá trước khi mua</h2><p>Claim một lần, chọn mã đã lưu ngay tại bước thanh toán.</p></div>{promotionNotice && <p className="hot-deals-promotion-notice">{promotionNotice}</p>}<div className="hot-deals-promotion-grid">{promotions.map((promotion) => <article className="hot-deals-promotion-card" key={promotion.id}><div><strong>{promotion.type === "PERCENTAGE" ? `${promotion.discountValue}% OFF` : promotion.type === "FIXED_AMOUNT" ? `Giảm ${dealMoney(promotion.discountValue)}` : "FREE SHIPPING"}</strong><span>Mã: <b>{promotion.code}</b></span><small>Đơn tối thiểu {dealMoney(promotion.minOrderAmount)}{promotion.maxDiscountAmount ? ` · Tối đa ${dealMoney(promotion.maxDiscountAmount)}` : ""}</small></div><button disabled={claimingId === promotion.id || claimedIds.has(promotion.id)} onClick={() => handleClaim(promotion)}>{claimedIds.has(promotion.id) ? "Đã claim" : claimingId === promotion.id ? "Đang lưu..." : "Claim"}</button></article>)}</div>{!promotions.length && <p className="hot-deals-empty">Hiện chưa có mã khuyến mãi đang hoạt động.</p>}</section>
    <section className="hot-deals-categories"><h2>Khám phá theo danh mục</h2><div>{categories.map((category, index) => { const Icon = icons[index % icons.length]; return <button key={category.id} onClick={() => chooseCategory(category)}><span><Icon /></span><b>{category.name}</b></button>; })}</div></section>
    <section className="hot-deals-content" id="hot-deals-products"><div className="hot-deals-title"><span><LocalOfferOutlinedIcon /> HOT DEAL MỖI NGÀY</span><h2>Săn deal nhanh · Giá tốt mỗi ngày</h2><p>Các sản phẩm này được lấy trực tiếp từ chỉ mục tìm kiếm và chỉ hiển thị hàng còn bán.</p></div><nav className="hot-deals-tabs"><button className="active" onClick={() => document.getElementById("hot-deals-products")?.scrollIntoView({ behavior: "smooth" })}>Tất cả</button>{categories.slice(0, 6).map((category) => <button key={category.id} onClick={() => chooseCategory(category)}>{category.name}</button>)}</nav><div className="hot-deals-grid">{deals.map((product, index) => <DealCard key={productId(product)} product={product} index={index} onClick={() => openProduct(product)} />)}</div>{loading && <p className="hot-deals-empty">Đang tải ưu đãi…</p>}{!loading && !deals.length && <p className="hot-deals-empty">Chưa có sản phẩm ưu đãi đang còn hàng.</p>}{featured.length > visible && <button className="hot-deals-more" onClick={() => setVisible((current) => Math.min(current + 8, featured.length))}>Xem thêm ưu đãi</button>}</section>
    <section className="hot-deals-category-sections">{categories.map((category) => { const items = categoryDeals[category.id] || []; if (!items.length) return null; return <section id={`hot-deals-category-${category.id}`} key={category.id} className="hot-deals-category-section"><header><span><LocalOfferOutlinedIcon /> DEAL THEO DANH MỤC</span><h2>{category.name}</h2><button onClick={() => navigate(`/shop/categories/${slugify(category.name)}-${category.id}`)}>Xem tất cả</button></header><div className="hot-deals-grid">{items.map((product, index) => <DealCard key={productId(product)} product={product} index={index} onClick={() => openProduct(product)} />)}</div></section>; })}</section>
  </main>;
}
