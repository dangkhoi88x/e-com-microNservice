import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Bell, Check, LogOut, Pencil, Save, Settings, Star, UserRound } from "lucide-react";
import { logout } from "../services/authenticationService";
import { getMyProfile, updateMyProfile } from "../services/profileService";
import { getMyNotifications } from "../services/notificationService";
import "./MyAccount.css";
import "./MyAccountNotifications.css";

const emptyProfile = { firstName: "", lastName: "", avatarUrl: "", bio: "", birthDate: "", phoneNumber: "", address: "", city: "", postalCode: "" };

export default function MyAccount() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [form, setForm] = useState(emptyProfile);
  const [notifications, setNotifications] = useState([]);
  const [notice, setNotice] = useState("");
  const [saving, setSaving] = useState(false);
  const activeTab = searchParams.get("tab") === "notifications" ? "notifications" : "profile";

  useEffect(() => {
    getMyProfile()
      .then((data) => setForm({
        firstName: data?.firstName || "",
        lastName: data?.lastName || "",
        avatarUrl: data?.avatarUrl || "",
        bio: data?.bio || "",
        birthDate: data?.birthDate || "",
        phoneNumber: data?.phoneNumber || "",
        address: data?.address || "",
        city: data?.city || "",
        postalCode: data?.postalCode || "",
      }))
      .catch(() => setNotice("Không thể tải hồ sơ. Bạn vẫn có thể nhập và lưu lại thông tin."));
  }, []);
  useEffect(() => { if (activeTab === "notifications") getMyNotifications().then(setNotifications).catch(() => setNotice("Không thể tải thông báo.")); }, [activeTab]);

  const name = `${form.firstName || "Khách"} ${form.lastName || "hàng"}`;
  const initial = (form.firstName || "N").charAt(0).toUpperCase();
  const change = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  const leave = async () => { await logout(); window.location.assign("/shop"); };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setNotice("");
    try {
      await updateMyProfile({ ...form, firstName: form.firstName.trim(), lastName: form.lastName.trim() });
      setNotice("Thông tin hồ sơ đã được cập nhật.");
    } catch (error) {
      setNotice(error.response?.data?.message || "Không thể cập nhật hồ sơ.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <main className="account-page">
      <section className="account-layout">
        <aside className="account-sidebar">
          <Link to="/shop" className="account-mobile-home">← Về cửa hàng</Link>
          <p className="account-sidebar-kicker">TÀI KHOẢN</p>
          <h1>Hồ sơ người dùng</h1>
          <nav className="account-menu" aria-label="Tài khoản">
            <button className={activeTab === "profile" ? "active" : ""} type="button" onClick={() => setSearchParams({})}><UserRound size={20} />Thông tin cá nhân</button>
            <button type="button" onClick={() => setNotice("Cài đặt thông báo sẽ sớm được cập nhật.")}><Settings size={20} />Cài đặt</button>
            <button className={activeTab === "notifications" ? "active" : ""} type="button" onClick={() => setSearchParams({ tab: "notifications" })}><Bell size={20} />Thông báo</button>
          </nav>
          <button className="account-logout" onClick={leave}><LogOut size={19} />Đăng xuất</button>
        </aside>

        <section className="account-content">
          <div className="account-profile-hero">
            <div className="account-avatar">
              {form.avatarUrl ? <img src={form.avatarUrl} alt={`Ảnh đại diện ${name}`} /> : initial}
              <span><Pencil size={14} /></span>
            </div>
            <div>
              <p>HỒ SƠ CỦA BẠN</p>
              <h2>{name}</h2>
              <small>{form.bio || "Thành viên NovaShop"}</small>
            </div>
            <div className="account-member"><Star size={15} /> Thành viên NovaShop</div>
          </div>

          {activeTab === "notifications" ? <section className="account-form account-notifications"><div className="account-form-heading"><div><p>THÔNG BÁO</p><h2>Cập nhật mới nhất</h2></div><span>{notifications.length} thông báo</span></div>{notifications.length ? <div className="account-notification-list">{notifications.map((item) => <article key={item.id}><Bell size={18} /><div><b>{item.title}</b><p>{item.message}</p><small>{item.createdAt ? new Date(item.createdAt).toLocaleString("vi-VN") : ""}</small></div></article>)}</div> : <p className="account-notification-empty">Bạn chưa có thông báo nào.</p>}</section> : <form className="account-form" onSubmit={submit}>
            <div className="account-form-heading">
              <div><p>THÔNG TIN CÁ NHÂN</p><h2>Chỉnh sửa hồ sơ</h2></div>
              <span>Những thay đổi sẽ được lưu vào tài khoản của bạn.</span>
            </div>
            <div className="account-form-fields">
              <label>Họ <b>*</b><input required name="firstName" value={form.firstName} onChange={change} placeholder="Nhập họ" /></label>
              <label>Tên <b>*</b><input required name="lastName" value={form.lastName} onChange={change} placeholder="Nhập tên" /></label>
              <label>Ảnh đại diện URL<input name="avatarUrl" value={form.avatarUrl} onChange={change} placeholder="https://..." /></label>
              <label>Số điện thoại<input name="phoneNumber" value={form.phoneNumber} onChange={change} placeholder="Ví dụ: 090 123 4567" /></label>
              <label className="wide">Địa chỉ<input name="address" value={form.address} onChange={change} placeholder="Số nhà, tên đường, phường/xã" /></label>
              <label>Thành phố / Tỉnh<input name="city" value={form.city} onChange={change} placeholder="Ví dụ: Hồ Chí Minh" /></label>
              <label>Mã bưu chính<input name="postalCode" value={form.postalCode} onChange={change} placeholder="Ví dụ: 700000" /></label>
              <label>Ngày sinh<input name="birthDate" type="date" value={form.birthDate} onChange={change} /></label>
              <label>Giới thiệu<textarea name="bio" value={form.bio} onChange={change} placeholder="Giới thiệu ngắn về bạn" rows="3" /></label>
            </div>
            {notice && <p className="account-notice"><Check size={16} />{notice}</p>}
            <button className="account-save" type="submit" disabled={saving}><Save size={17} />{saving ? "Đang lưu..." : "Lưu thay đổi"}</button>
          </form>}
        </section>
      </section>
    </main>
  );
}
