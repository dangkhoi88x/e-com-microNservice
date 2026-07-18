import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ClipboardList, Heart, LogOut, MapPin, Package, Pencil, Settings, ShoppingBag } from "lucide-react";
import { logout } from "../services/authenticationService";
import { getMyProfile } from "../services/profileService";
import "./MyAccount.css";

export default function MyAccount() {
  const [profile, setProfile] = useState(null);
  useEffect(() => { getMyProfile().then(setProfile).catch(() => setProfile({})); }, []);
  const name = `${profile?.firstName || "Khách"} ${profile?.lastName || "hàng"}`;
  const initial = (profile?.firstName || "N").charAt(0).toUpperCase();
  const leave = () => { logout(); window.location.assign("/shop"); };
  return <main className="account-page"><header className="account-top"><Link to="/shop" className="account-brand"><i>N</i>Nova<span>Shop</span></Link><Link to="/shop"><ShoppingBag size={18} /> Tiếp tục mua sắm</Link></header><section className="account-shell"><div className="account-heading"><p>TÀI KHOẢN CỦA TÔI</p><h1>Chào, {profile ? name : "bạn"}!</h1><span>Quản lý hồ sơ và hành trình mua sắm của bạn.</span></div><section className="account-profile"><div className="account-avatar">{profile?.avatarUrl ? <img src={profile.avatarUrl} alt=""/> : initial}</div><div><h2>{name}</h2><p>{profile?.bio || "Thành viên NovaShop"}</p></div><Link to="/shop/account/profile"><Pencil size={15}/> Chỉnh sửa hồ sơ</Link></section><section className="account-grid"><Link to="/shop/orders"><span><ClipboardList size={22}/></span><div><h2>Đơn hàng của tôi</h2><p>Theo dõi, thanh toán lại và mua lại.</p></div></Link><button type="button"><span><MapPin size={22}/></span><div><h2>Sổ địa chỉ</h2><p>Quản lý địa chỉ giao hàng của bạn.</p></div></button><button type="button"><span><Heart size={22}/></span><div><h2>Sản phẩm yêu thích</h2><p>Lưu những món bạn muốn mua sau.</p></div></button><Link to="/shop/account/profile"><span><Settings size={22}/></span><div><h2>Cài đặt tài khoản</h2><p>Cập nhật thông tin cá nhân và bảo mật.</p></div></Link></section><section className="account-help"><Package size={22}/><p><b>Cần hỗ trợ về đơn hàng?</b>NovaShop luôn sẵn sàng hỗ trợ bạn 24/7.</p><Link to="/shop/orders">Xem đơn hàng</Link></section><button className="account-logout" onClick={leave}><LogOut size={17}/> Đăng xuất</button></section></main>;
}
