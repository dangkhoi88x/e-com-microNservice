import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft, BadgeCheck, Minus, PackageOpen, Plus, ShieldCheck, ShoppingBag, Trash2, Truck } from "lucide-react";
import { getMyCart, removeCartItem, updateCartItem } from "../services/cartService";
import { isAuthenticated } from "../services/authenticationService";
import "./Cart.css";

const money = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value || 0);

export default function Cart() {
  const navigate = useNavigate();
  const [cart, setCart] = useState({ items: [], totalAmount: 0 });
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState("");
  const items = cart.items || [];
  const subtotal = useMemo(() => Number(cart.totalAmount || 0), [cart.totalAmount]);
  const shipping = subtotal >= 500000 ? 0 : 30000;

  const load = async () => {
    try {
      setCart(await getMyCart());
    } catch {
      setNotice("Không thể tải giỏ hàng. Vui lòng đăng nhập lại.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { if (isAuthenticated()) load(); else { setLoading(false); setNotice("Vui lòng đăng nhập để sử dụng giỏ hàng."); } }, []);

  const changeQuantity = async (item, amount) => {
    try {
      const next = Number(item.quantity || 1) + amount <= 0
        ? await removeCartItem(item.id)
        : await updateCartItem(item.id, { quantity: Number(item.quantity || 1) + amount, selected: item.selected });
      setCart(next);
    } catch (error) {
      setNotice(error.response?.data?.message || "Không thể cập nhật giỏ hàng.");
    }
  };

  const remove = async (itemId) => {
    try { setCart(await removeCartItem(itemId)); } catch (error) { setNotice(error.response?.data?.message || "Không thể xóa sản phẩm."); }
  };

  if (loading) return <main className="cart-empty"><div><PackageOpen size={48} /><h1>Đang tải giỏ hàng…</h1></div></main>;
  if (!isAuthenticated() || !items.length) return <main className="cart-empty"><div><PackageOpen size={48} /><p>GIỎ HÀNG NOVASHOP</p><h1>{isAuthenticated() ? "Giỏ hàng của bạn đang trống" : "Đăng nhập để xem giỏ hàng"}</h1><span>{notice || "Chọn sản phẩm bạn yêu thích và bắt đầu hành trình mua sắm."}</span><Link to={isAuthenticated() ? "/shop" : "/shop/login?redirect=/cart"}><ShoppingBag size={18} /> {isAuthenticated() ? "Khám phá cửa hàng" : "Đăng nhập"}</Link></div></main>;

  return <main className="cart-page">
    <header className="cart-header"><Link className="cart-brand" to="/shop"><i>N</i> Nova<span>Shop</span></Link><div><ShieldCheck size={17} /> Mua sắm bảo mật</div></header>
    <section className="cart-shell">
      <Link className="cart-back" to="/shop"><ArrowLeft size={17} /> Tiếp tục mua sắm</Link>
      <div className="cart-title"><div><p>GIỎ HÀNG</p><h1>Sản phẩm đã chọn</h1><span>{items.length} sản phẩm đang chờ thanh toán</span></div><div className="cart-benefit"><Truck size={20} /><span>Miễn phí vận chuyển<br /><b>cho đơn từ 500.000 ₫</b></span></div></div>
      <div className="cart-layout"><section className="cart-list">{items.map((item) => <article className="cart-item" key={item.id}><div className="cart-image"><img src={item.imageUrl || "https://placehold.co/160x160/e7f2f8/3b82c4?text=Nova"} alt={item.productName} /></div><div className="cart-item-info"><p>{item.variantName || "NovaShop selection"}</p><h2>{item.productName}</h2><strong>{money(item.price)}</strong><div className="cart-item-actions"><div className="cart-quantity"><button aria-label="Giảm số lượng" onClick={() => changeQuantity(item, -1)}><Minus size={14} /></button><b>{item.quantity}</b><button aria-label="Tăng số lượng" onClick={() => changeQuantity(item, 1)}><Plus size={14} /></button></div><button className="cart-remove" onClick={() => remove(item.id)}><Trash2 size={16} /> Xoá</button></div></div><b className="cart-line-total">{money(item.subtotal || Number(item.price || 0) * item.quantity)}</b></article>)}</section>
        <aside className="cart-summary"><div className="cart-summary-top"><p>TÓM TẮT ĐƠN HÀNG</p><h2>Thanh toán</h2></div><div className="cart-summary-row"><span>Tạm tính</span><b>{money(subtotal)}</b></div><div className="cart-summary-row"><span>Vận chuyển</span><b className={shipping === 0 ? "cart-free" : ""}>{shipping === 0 ? "Miễn phí" : money(shipping)}</b></div>{subtotal < 500000 && <div className="cart-shipping-tip"><Truck size={17} /> Thêm <b>{money(500000 - subtotal)}</b> để được miễn phí vận chuyển.</div>}<div className="cart-summary-total"><span>Tổng cộng</span><b>{money(subtotal + shipping)}</b></div><button className="cart-checkout" onClick={() => navigate("/checkout")}>Tiến hành thanh toán <span>→</span></button><div className="cart-guarantee"><BadgeCheck size={20} /><p><b>Mua sắm an tâm</b><br />Sản phẩm được kiểm tra kỹ lưỡng trước khi giao.</p></div></aside></div>
    </section>
  </main>;
}
