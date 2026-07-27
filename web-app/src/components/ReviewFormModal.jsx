import { useState } from "react";
import { Star, Trash2, X } from "lucide-react";
import { createReview, updateReview } from "../services/reviewService";
import { deleteMedia, uploadReviewImage } from "../services/mediaService";
import "./ReviewFormModal.css";

export default function ReviewFormModal({ orderItem, review, onClose, onSaved }) {
  const [rating, setRating] = useState(review?.rating || 5);
  const [content, setContent] = useState(review?.content || "");
  const [images, setImages] = useState((review?.images || []).map((url, index) => ({ id: `existing-${index}-${url}`, url })));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const selectImages = (event) => {
    const files = Array.from(event.target.files || []);
    event.target.value = "";
    const validFiles = files.filter((file) => ["image/jpeg", "image/png", "image/webp"].includes(file.type) && file.size <= 6 * 1024 * 1024);
    if (validFiles.length !== files.length) setError("Chỉ chọn JPEG, PNG hoặc WebP, mỗi ảnh tối đa 6 MB.");
    setImages((current) => [...current, ...validFiles.slice(0, Math.max(0, 5 - current.length)).map((file) => ({ id: `${file.name}-${file.lastModified}-${crypto.randomUUID()}`, file, previewUrl: URL.createObjectURL(file) }))]);
  };

  const removeImage = (imageId) => setImages((current) => {
    const image = current.find((item) => item.id === imageId);
    if (image?.previewUrl) URL.revokeObjectURL(image.previewUrl);
    return current.filter((item) => item.id !== imageId);
  });

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    const uploadedMedia = [];
    try {
      const imageUrls = [];
      for (const image of images) {
        if (image.file) {
          const media = await uploadReviewImage(image.file);
          uploadedMedia.push(media);
          imageUrls.push(media.contentUrl);
        } else imageUrls.push(image.url);
      }
      const payload = { rating, content: content.trim() || null, images: imageUrls };
      const saved = review
        ? await updateReview(review.id, payload)
        : await createReview({ ...payload, orderItemId: orderItem.id });
      const retainedUrls = new Set(imageUrls);
      await Promise.allSettled((review?.images || [])
        .filter((url) => !retainedUrls.has(url))
        .map((url) => url.match(/\/api\/v1\/media\/([0-9a-f-]{36})\/content(?:$|[?#])/i)?.[1])
        .filter(Boolean)
        .map((mediaId) => deleteMedia(mediaId)));
      onSaved(saved);
    } catch (requestError) {
      uploadedMedia.forEach((media) => deleteMedia(media.id).catch(() => {}));
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
      <label>Ảnh đánh giá <small>JPEG, PNG hoặc WebP · tối đa 6 MB/ảnh · tối đa 5 ảnh</small><input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={selectImages} disabled={images.length >= 5}/></label>
      {images.length > 0 && <div className="review-form-image-list">{images.map((image) => <div key={image.id}><img src={image.previewUrl || image.url} alt="Ảnh đánh giá xem trước"/><button type="button" onClick={() => removeImage(image.id)} aria-label="Xóa ảnh đánh giá"><Trash2 size={15}/></button></div>)}</div>}
      {error && <div className="review-form-error">{error}</div>}
      <div className="review-modal-actions"><button type="button" onClick={onClose}>Để sau</button><button disabled={saving}>{saving ? "Đang lưu..." : "Gửi đánh giá"}</button></div>
    </form>
  </div>;
}
