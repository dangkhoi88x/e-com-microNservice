import { useCallback, useEffect, useMemo, useState } from "react";
import { CheckCircle2, Image, MessageSquareText, Star } from "lucide-react";
import { isAuthenticated } from "../services/authenticationService";
import { getMyOrders } from "../services/orderService";
import { getMyReviews, getProductReviews, getProductReviewSummary } from "../services/reviewService";
import ReviewFormModal from "./ReviewFormModal";
import "./ProductReviews.css";

const date = (value) => value ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium" }).format(new Date(value)) : "";
const reviewer = (review) => review.reviewerName || "Khách hàng NovaShop";

export default function ProductReviews({ product, onSummaryChange }) {
  const [reviews, setReviews] = useState([]);
  const [summary, setSummary] = useState({ averageRating: 0, reviewCount: 0, ratingDistribution: {} });
  const [eligibleItem, setEligibleItem] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [reviewPage, reviewSummary] = await Promise.all([
        getProductReviews(product.id, { page: 1, size: 20 }),
        getProductReviewSummary(product.id),
      ]);
      setReviews(reviewPage.content || []);
      setSummary(reviewSummary);
      onSummaryChange?.(reviewSummary);

      if (isAuthenticated()) {
        const [orders, mine] = await Promise.all([
          getMyOrders({ page: 1, size: 100 }),
          getMyReviews({ page: 1, size: 100 }),
        ]);
        const reviewedIds = new Set((mine.content || []).map((review) => review.orderItemId));
        const candidate = (orders.content || [])
          .filter((order) => order.status === "COMPLETED")
          .flatMap((order) => order.items || [])
          .find((item) => item.id && item.productId === product.id && !reviewedIds.has(item.id));
        setEligibleItem(candidate || null);
      } else {
        setEligibleItem(null);
      }
    } catch {
      setReviews([]);
      setSummary({ averageRating: 0, reviewCount: 0, ratingDistribution: {} });
      onSummaryChange?.({ averageRating: 0, reviewCount: 0, ratingDistribution: {} });
      setEligibleItem(null);
    } finally {
      setLoading(false);
    }
  }, [onSummaryChange, product.id]);

  useEffect(() => { load(); }, [load]);
  const maxCount = useMemo(() => Math.max(1, ...Object.values(summary.ratingDistribution || {}).map(Number)), [summary]);

  return <section className="product-reviews-section">
    <div className="product-reviews-heading"><div><p>ĐÁNH GIÁ ĐÃ XÁC MINH</p><h2>Khách hàng nói gì về sản phẩm?</h2></div>{eligibleItem && <button onClick={() => setFormOpen(true)}><MessageSquareText size={16}/> Viết đánh giá</button>}</div>
    <div className="product-review-overview">
      <article className="review-score"><strong>{Number(summary.averageRating || 0).toFixed(1)}</strong><div>{[1,2,3,4,5].map((star) => <Star key={star} size={18} fill={star <= Math.round(summary.averageRating || 0) ? "currentColor" : "none"}/>)}</div><span>{summary.reviewCount || 0} đánh giá</span></article>
      <div className="review-bars">{[5,4,3,2,1].map((rating) => { const count = Number(summary.ratingDistribution?.[rating] || 0); return <div key={rating}><span>{rating} <Star size={11} fill="currentColor"/></span><i><b style={{ width: `${count * 100 / maxCount}%` }}/></i><small>{count}</small></div>; })}</div>
    </div>
    {loading ? <div className="product-reviews-empty">Đang tải đánh giá...</div> : reviews.length ? <div className="product-review-list">{reviews.map((review) => <article key={review.id}><header><div className="review-avatar">{reviewer(review).charAt(0) || "N"}</div><p><b>{reviewer(review)}</b>{review.verifiedPurchase && <span><CheckCircle2 size={13}/> Đã mua hàng · {date(review.createdAt)}</span>}</p><div>{[1,2,3,4,5].map((star) => <Star key={star} size={14} fill={star <= review.rating ? "currentColor" : "none"}/>)}</div></header>{review.content && <p className="review-content">{review.content}</p>}{review.images?.length > 0 && <div className="review-images">{review.images.map((url) => <a href={url} target="_blank" rel="noreferrer" key={url}>{url ? <img src={url} alt="Ảnh đánh giá"/> : <Image/>}</a>)}</div>}{review.sellerReply && <blockquote><b>NovaShop phản hồi</b>{review.sellerReply}</blockquote>}</article>)}</div> : <div className="product-reviews-empty"><MessageSquareText size={34}/><h3>Chưa có đánh giá</h3><p>Hãy là khách hàng đầu tiên chia sẻ trải nghiệm.</p></div>}
    {formOpen && <ReviewFormModal orderItem={eligibleItem} onClose={() => setFormOpen(false)} onSaved={() => { setFormOpen(false); load(); }}/>}
  </section>;
}
