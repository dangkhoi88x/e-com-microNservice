import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Check, Save } from "lucide-react";
import { getMyProfile, updateMyProfile } from "../services/profileService";
import "./CustomerProfile.css";

export default function CustomerProfile() {
  const [form, setForm] = useState({ firstName: "", lastName: "", avatarUrl: "", bio: "", birthDate: "" });
  const [notice, setNotice] = useState("");
  const [saving, setSaving] = useState(false);
  useEffect(() => { getMyProfile().then((data) => setForm({ firstName: data?.firstName || "", lastName: data?.lastName || "", avatarUrl: data?.avatarUrl || "", bio: data?.bio || "", birthDate: data?.birthDate || "" })).catch(() => setNotice("Không thể tải hồ sơ.")); }, []);
  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  const submit = async (event) => { event.preventDefault(); setSaving(true); setNotice(""); try { await updateMyProfile(form); setNotice("Hồ sơ đã được cập nhật."); } catch (error) { setNotice(error.response?.data?.message || "Không thể cập nhật hồ sơ."); } finally { setSaving(false); } };
  return <main className="customer-profile-page"><header><Link to="/shop/account"><ArrowLeft size={17}/> Quay lại tài khoản</Link><Link className="customer-profile-brand" to="/shop"><i>N</i>Nova<span>Shop</span></Link></header><form className="customer-profile-card" onSubmit={submit}><p>HỒ SƠ CÁ NHÂN</p><h1>Thông tin của bạn</h1><span>Cập nhật thông tin để NovaShop phục vụ bạn tốt hơn.</span><div className="customer-profile-fields"><label>Họ <b>*</b><input required name="firstName" value={form.firstName} onChange={update}/></label><label>Tên <b>*</b><input required name="lastName" value={form.lastName} onChange={update}/></label><label className="full">Ảnh đại diện URL<input name="avatarUrl" value={form.avatarUrl} onChange={update}/></label><label className="full">Giới thiệu<textarea name="bio" value={form.bio} onChange={update}/></label><label>Ngày sinh<input name="birthDate" type="date" value={form.birthDate} onChange={update}/></label></div>{notice && <div className="customer-profile-notice"><Check size={16}/>{notice}</div>}<button type="submit" disabled={saving}><Save size={17}/>{saving ? "Đang lưu..." : "Lưu thay đổi"}</button></form></main>;
}
