import { useState } from "react";
import { Star, X } from "lucide-react";
import { createReview, updateReview } from "../services/reviewService";
import "./ReviewFormModal.css";

export default function ReviewFormModal({ orderItem, review, onClose, onSaved }) {
  const [rating, setRating] = useState(review?.rating || 5);
  const [content, setContent] = useState(review?.content || "");
  const [images, setImages] = useState((review?.images || []).join("\n"));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    const payload = {
      rating,
      content: content.trim() || null,
      images: images.split(/\r?\n|,/).map((value) => value.trim()).filter(Boolean).slice(0, 5),
    };
    try {
      const saved = review
        ? await updateReview(review.id, payload)
        : await createReview({ ...payload, orderItemId: orderItem.id });
      onSaved(saved);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể lưu đánh giá lúc này.");
    } finally {
      setSaving(false);
    }
  };

  return <div className="review-modal-backdrop" onMouseDown={onClose}>
    <form className="review-modal" onSubmit={submit} onMouseDown={(event) => event.stopPropagation()}>
      <button type="button" className="review-modal-close" onClick={onClose}><X size={18}/></button>
      <p>TRẢI NGHIỆM SẢN PHẨM</p>
      <h2>{review ? "Chỉnh sửa đánh giá" : "Viết đánh giá"}</h2>
      <span>{orderItem?.productName}</span>
      <div className="review-star-picker" aria-label="Chọn số sao">
        {[1, 2, 3, 4, 5].map((value) => <button type="button" key={value} onClick={() => setRating(value)} aria-label={`${value} sao`}><Star fill={value <= rating ? "currentColor" : "none"}/></button>)}
      </div>
      <textarea value={content} maxLength={3000} onChange={(event) => setContent(event.target.value)} placeholder="Chia sẻ cảm nhận về sản phẩm..." rows={5}/>
      <label>Ảnh đánh giá <small>Mỗi URL một dòng, tối đa 5 ảnh</small><textarea value={images} onChange={(event) => setImages(event.target.value)} placeholder="https://..." rows={3}/></label>
      {error && <div className="review-form-error">{error}</div>}
      <div className="review-modal-actions"><button type="button" onClick={onClose}>Để sau</button><button disabled={saving}>{saving ? "Đang lưu..." : "Gửi đánh giá"}</button></div>
    </form>
  </div>;
}
