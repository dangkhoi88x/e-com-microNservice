import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Check, CloudUpload, Save, Trash2 } from "lucide-react";
import { getMyProfile, updateMyProfile } from "../services/profileService";
import { deleteMedia, uploadAvatar } from "../services/mediaService";
import "./CustomerProfile.css";

export default function CustomerProfile() {
  const [form, setForm] = useState({ firstName: "", lastName: "", avatarUrl: "", bio: "", birthDate: "" });
  const [notice, setNotice] = useState("");
  const [saving, setSaving] = useState(false);
  const [avatarFile, setAvatarFile] = useState(null);
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState("");
  useEffect(() => { getMyProfile().then((data) => setForm({ firstName: data?.firstName || "", lastName: data?.lastName || "", avatarUrl: data?.avatarUrl || "", bio: data?.bio || "", birthDate: data?.birthDate || "" })).catch(() => setNotice("Không thể tải hồ sơ.")); }, []);
  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  const selectAvatar = (event) => {
    const file = event.target.files?.[0]; event.target.value = "";
    if (!file) return;
    if (!new Set(["image/jpeg", "image/png", "image/webp"]).has(file.type) || file.size > 1024 * 1024) { setNotice("Ảnh đại diện phải là JPEG, PNG hoặc WebP và không quá 1 MB."); return; }
    if (avatarPreviewUrl) URL.revokeObjectURL(avatarPreviewUrl);
    setAvatarFile(file); setAvatarPreviewUrl(URL.createObjectURL(file));
  };
  const clearAvatar = () => { if (avatarPreviewUrl) URL.revokeObjectURL(avatarPreviewUrl); setAvatarFile(null); setAvatarPreviewUrl(""); setForm((current) => ({ ...current, avatarUrl: "" })); };
  const submit = async (event) => { event.preventDefault(); setSaving(true); setNotice(""); let uploadedMedia; try { const avatarUrl = avatarFile ? (uploadedMedia = await uploadAvatar(avatarFile)).contentUrl : form.avatarUrl; await updateMyProfile({ ...form, avatarUrl }); const oldMediaId = form.avatarUrl !== avatarUrl && form.avatarUrl?.match(/\/api\/v1\/media\/([0-9a-f-]{36})\/content(?:$|[?#])/i)?.[1]; if (oldMediaId) deleteMedia(oldMediaId).catch(() => {}); setForm((current) => ({ ...current, avatarUrl })); if (avatarPreviewUrl) URL.revokeObjectURL(avatarPreviewUrl); setAvatarFile(null); setAvatarPreviewUrl(""); setNotice("Hồ sơ đã được cập nhật."); } catch (error) { if (uploadedMedia) deleteMedia(uploadedMedia.id).catch(() => {}); setNotice(error.response?.data?.message || "Không thể cập nhật hồ sơ."); } finally { setSaving(false); } };
  return <main className="customer-profile-page"><header><Link to="/shop/account"><ArrowLeft size={17}/> Quay lại tài khoản</Link><Link className="customer-profile-brand" to="/shop"><i>N</i>Nova<span>Shop</span></Link></header><form className="customer-profile-card" onSubmit={submit}><p>HỒ SƠ CÁ NHÂN</p><h1>Thông tin của bạn</h1><span>Cập nhật thông tin để NovaShop phục vụ bạn tốt hơn.</span><div className="customer-profile-fields"><label>Họ <b>*</b><input required name="firstName" value={form.firstName} onChange={update}/></label><label>Tên <b>*</b><input required name="lastName" value={form.lastName} onChange={update}/></label><label className="full">Ảnh đại diện<input type="file" accept="image/jpeg,image/png,image/webp" onChange={selectAvatar}/>{(avatarPreviewUrl || form.avatarUrl) && <button type="button" onClick={clearAvatar}><Trash2 size={15}/> Xóa ảnh</button>}{avatarPreviewUrl && <img src={avatarPreviewUrl} alt="Xem trước ảnh đại diện"/>}<small><CloudUpload size={15}/> JPEG, PNG hoặc WebP · tối đa 1 MB</small></label><label className="full">Giới thiệu<textarea name="bio" value={form.bio} onChange={update}/></label><label>Ngày sinh<input name="birthDate" type="date" value={form.birthDate} onChange={update}/></label></div>{notice && <div className="customer-profile-notice"><Check size={16}/>{notice}</div>}<button type="submit" disabled={saving}><Save size={17}/>{saving ? "Đang lưu..." : "Lưu thay đổi"}</button></form></main>;
}
