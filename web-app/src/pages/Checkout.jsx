import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  BadgeCheck,
  Check,
  ChevronRight,
  CircleAlert,
  CreditCard,
  LockKeyhole,
  MapPin,
  Minus,
  PackageCheck,
  Plus,
  ShieldCheck,
  TicketPercent,
  Truck,
  WalletCards,
} from "lucide-react";
import { getActiveProductSales, getClaimedPromotions, previewPromotion } from "../services/promotionService";
import { checkoutOrder } from "../services/orderService";
import { createPayment } from "../services/paymentService";
import { isAuthenticated } from "../services/authenticationService";
import { getMyCart, removeCartItem, updateCartItem } from "../services/cartService";
import "./Checkout.css";
import "./CheckoutQuantityEditor.css";

const money = (value) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value || 0);

const paymentMethods = [
  { value: "VNPAY", title: "VNPay", description: "Thanh toán an toàn qua VNPay", icon: CreditCard, mark: "V" },
  { value: "MOMO", title: "Ví MoMo", description: "Mở ứng dụng MoMo để xác nhận", icon: WalletCards, mark: "M" },
  { value: "BANK_TRANSFER", title: "Chuyển khoản ngân hàng", description: "Xác nhận thanh toán nhanh chóng", icon: BadgeCheck, mark: "BK" },
  { value: "COD", title: "Thanh toán khi nhận hàng", description: "Thanh toán bằng tiền mặt khi giao", icon: Truck, mark: "COD" },
];

function CheckoutQuantityEditor({ cart, onChange, priceOf }) {
  return <div className="checkout-edit-items">{cart.map((item) => <div className="checkout-edit-item" key={item.id}><img src={item.imageUrl || "https://placehold.co/120x120/e7f2f8/3b82c4?text=Nova"} alt={item.productName} /><div><h3>{item.productName}</h3><strong>{money(priceOf(item) * (item.quantity || 1))}</strong></div><div className="checkout-quantity" aria-label={`Quantity ${item.productName}`}><button type="button" onClick={() => onChange(item.id, -1)} aria-label={`Giảm số lượng hoặc xóa ${item.productName}`}><Minus size={13} /></button><b>{item.quantity || 1}</b><button type="button" onClick={() => onChange(item.id, 1)}><Plus size={13} /></button></div></div>)}</div>;
}

export default function Checkout() {
  const navigate = useNavigate();
  const [cart, setCart] = useState([]);
  const [method, setMethod] = useState("VNPAY");
  const [coupon, setCoupon] = useState("");
  const [couponApplied, setCouponApplied] = useState(false);
  const [promotionCalculation, setPromotionCalculation] = useState(null);
  const [couponLoading, setCouponLoading] = useState(false);
  const [claimedPromotions, setClaimedPromotions] = useState([]);
  const [flashDeals, setFlashDeals] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState("");
  const [form, setForm] = useState({
    email: "",
    firstName: "",
    lastName: "",
    address: "",
    ward: "",
    city: "",
    phone: "",
  });

  const flashPrice = (item) => flashDeals.flatMap((deal) => (deal.items || []).map((dealItem) => ({ ...dealItem, saleType: deal.saleType || "FLASH" }))).filter((dealItem) => dealItem.productId === item.productId && (dealItem.variantId || null) === (item.variantId || null)).sort((a, b) => Number(a.salePrice) - Number(b.salePrice) || (a.saleType === "FLASH" ? -1 : 1) - (b.saleType === "FLASH" ? -1 : 1))[0]?.salePrice;
  const itemPrice = (item) => Number(flashPrice(item) ?? item.price ?? 0);
  const subtotal = cart.reduce(
    (total, item) => total + itemPrice(item) * (item.quantity || 1),
    0,
  );
  const shipping = subtotal > 0 ? 30000 : 0;
  const discount = promotionCalculation?.discountAmount || 0;
  const total = Math.max(0, subtotal - discount + shipping);
  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  useEffect(() => {
    getMyCart().then((data) => setCart(data.items || [])).catch(() => setNotice("Không thể tải giỏ hàng. Vui lòng đăng nhập lại."));
    getClaimedPromotions().then(setClaimedPromotions).catch(() => setClaimedPromotions([]));
    getActiveProductSales().then(setFlashDeals).catch(() => setFlashDeals([]));
  }, []);

  const applyCoupon = async () => {
    const code = coupon.trim();
    if (!code) return setNotice("Hãy nhập mã ưu đãi trước khi áp dụng.");
    setCouponLoading(true);
    setNotice("");
    try {
      const calculation = await previewPromotion(code, subtotal);
      setPromotionCalculation(calculation);
      setCouponApplied(true);
    } catch (error) {
      setPromotionCalculation(null);
      setCouponApplied(false);
      setNotice(error.response?.data?.message || "Mã ưu đãi không hợp lệ hoặc chưa đủ điều kiện.");
    } finally {
      setCouponLoading(false);
    }
  };
  const changeQuantity = async (itemId, amount) => {
    const item = cart.find((entry) => entry.id === itemId);
    if (!item) return;
    try {
      const updated = Number(item.quantity || 1) + amount <= 0
        ? await removeCartItem(itemId)
        : await updateCartItem(itemId, { quantity: Number(item.quantity || 1) + amount, selected: item.selected });
      setCart(updated.items || []);
      setCouponApplied(false);
      setPromotionCalculation(null);
    } catch (error) {
      setNotice(error.response?.data?.message || "Không thể cập nhật giỏ hàng.");
    }
  };

  const handleCheckout = async (event) => {
    event.preventDefault();
    setNotice("");
    if (!cart.length) return setNotice("Giỏ hàng đang trống. Hãy chọn sản phẩm trước khi thanh toán.");
    if (!isAuthenticated()) return navigate("/shop/login?redirect=/checkout");

    const shippingAddress = [form.address, form.ward, form.city].filter(Boolean).join(", ");
    if (!form.email || !form.firstName || !form.lastName || !shippingAddress || !form.phone) {
      return setNotice("Hãy điền đầy đủ thông tin giao hàng bắt buộc.");
    }

    setSubmitting(true);
    try {
      const order = await checkoutOrder({
        shippingAddress,
        campaignCode: couponApplied ? coupon : null,
      });
      if (order?.status !== "PENDING_PAYMENT") {
        throw new Error(order?.status === "INVENTORY_FAILED"
          ? "Một số sản phẩm không còn đủ tồn kho. Vui lòng điều chỉnh giỏ hàng rồi thử lại."
          : "Đơn hàng chưa sẵn sàng để thanh toán. Vui lòng thử lại sau.");
      }
      const payment = await createPayment({ orderId: order.id, method });
      navigate(`/payments?paymentId=${payment?.id || ""}`, { state: { order, payment, method } });
    } catch (error) {
      setNotice(error.response?.data?.message || error.response?.data?.error || error.message || "Không thể khởi tạo thanh toán. Vui lòng thử lại sau.");
    } finally {
      setSubmitting(false);
    }
  };

  if (!cart.length) {
    return (
      <main className="checkout-empty">
        <div className="checkout-empty-card">
          <PackageCheck size={46} />
          <p className="checkout-eyebrow">NovaShop checkout</p>
          <h1>Giỏ hàng của bạn đang trống</h1>
          <p>Khám phá những sản phẩm được chọn lọc và thêm món bạn yêu thích vào giỏ hàng.</p>
          <Link to="/shop" className="checkout-primary"><ArrowLeft size={18} /> Tiếp tục mua sắm</Link>
        </div>
      </main>
    );
  }

  return (
    <main className="checkout-page">
      <header className="checkout-header">
        <Link className="checkout-brand" to="/shop"><span className="checkout-brand-mark">N</span> Nova<span>Shop</span></Link>
        <div className="checkout-secure"><LockKeyhole size={16} /> Thanh toán bảo mật</div>
      </header>

      <section className="checkout-shell">
        <div className="checkout-intro">
          <Link to="/shop" className="checkout-back"><ArrowLeft size={16} /> Quay lại cửa hàng</Link>
          <p className="checkout-eyebrow">CHECKOUT</p>
          <h1>Hoàn tất đơn hàng của bạn</h1>
          <p>Chỉ còn một bước nữa để những món bạn chọn lên đường đến với bạn.</p>
          <ol className="checkout-steps">
            <li className="is-active"><span><Check size={15} /></span> Giỏ hàng</li>
            <li className="is-active"><span>2</span> Thông tin giao hàng</li>
            <li><span>3</span> Thanh toán</li>
          </ol>
        </div>

        <form className="checkout-layout" onSubmit={handleCheckout}>
          <div className="checkout-main">
            <section className="checkout-card">
              <div className="checkout-card-title"><span className="checkout-icon"><MapPin size={19} /></span><div><h2>Giao đến đâu?</h2><p>Thông tin này giúp NovaShop giao đơn chính xác.</p></div></div>
              <div className="checkout-fields">
                <label className="checkout-field full"><span>Email nhận xác nhận <b>*</b></span><input required name="email" type="email" value={form.email} onChange={update} placeholder="ban@example.com" /></label>
                <label className="checkout-field"><span>Họ <b>*</b></span><input required name="firstName" value={form.firstName} onChange={update} placeholder="Nguyễn" /></label>
                <label className="checkout-field"><span>Tên <b>*</b></span><input required name="lastName" value={form.lastName} onChange={update} placeholder="Minh Anh" /></label>
                <label className="checkout-field full"><span>Địa chỉ nhận hàng <b>*</b></span><input required name="address" value={form.address} onChange={update} placeholder="Số nhà, tên đường" /></label>
                <label className="checkout-field"><span>Phường / Xã</span><input name="ward" value={form.ward} onChange={update} placeholder="Phường Bến Nghé" /></label>
                <label className="checkout-field"><span>Tỉnh / Thành phố <b>*</b></span><input required name="city" value={form.city} onChange={update} placeholder="TP. Hồ Chí Minh" /></label>
                <label className="checkout-field full"><span>Số điện thoại <b>*</b></span><input required name="phone" type="tel" value={form.phone} onChange={update} placeholder="090 123 4567" /></label>
              </div>
            </section>

            <section className="checkout-card">
              <div className="checkout-card-title"><span className="checkout-icon"><WalletCards size={19} /></span><div><h2>Chọn phương thức thanh toán</h2><p>Bạn có thể chọn phương thức phù hợp nhất.</p></div></div>
              <div className="checkout-methods">
                {paymentMethods.map((item) => {
                  const Icon = item.icon;
                  return <button type="button" key={item.value} onClick={() => setMethod(item.value)} className={`checkout-method ${method === item.value ? "is-selected" : ""}`}>
                    <span className="checkout-radio">{method === item.value && <Check size={14} />}</span><span className="checkout-method-logo"><Icon size={18} /><i>{item.mark}</i></span><span><strong>{item.title}</strong><small>{item.description}</small></span><ChevronRight size={18} className="checkout-method-chevron" />
                  </button>;
                })}
              </div>
            </section>
          </div>

          <aside className="checkout-summary-wrap">
            <section className="checkout-summary">
              <CheckoutQuantityEditor cart={cart} onChange={changeQuantity} priceOf={itemPrice} />
              <div className="checkout-summary-head"><div><p>Đơn hàng của bạn</p><h2>{cart.length} sản phẩm</h2></div><Link to="/shop">Chỉnh sửa</Link></div>
              <div className="checkout-items">
                {cart.map((item) => <div className="checkout-item" key={item.id}><div className="checkout-item-image"><img src={item.imageUrl || "https://placehold.co/120x120/e7f2f8/3b82c4?text=Nova"} alt={item.productName} /><b>×{item.quantity}</b></div><div><h3>{item.productName}</h3><p>{item.variantName || "NovaShop selection"}</p><strong>{money(itemPrice(item) * (item.quantity || 1))}</strong></div></div>)}
              </div>
              <div className="checkout-coupon"><TicketPercent size={18} />{claimedPromotions.length ? <select className="checkout-coupon-select" value={coupon} onChange={(event) => { setCoupon(event.target.value); setCouponApplied(false); setPromotionCalculation(null); setNotice(""); }}><option value="">Chọn mã đã claim</option>{claimedPromotions.map((promotion) => <option key={promotion.id} value={promotion.code}>{promotion.code} · {promotion.type === "PERCENTAGE" ? `${promotion.discountValue}%` : money(promotion.discountValue)}</option>)}</select> : <input value={coupon} onChange={(event) => { setCoupon(event.target.value); setCouponApplied(false); setPromotionCalculation(null); setNotice(""); }} placeholder="Bạn chưa claim mã nào" />}<button type="button" onClick={applyCoupon} disabled={couponLoading || !coupon.trim()}>{couponLoading ? "Đang kiểm tra..." : "Áp dụng"}</button></div>
              {couponApplied && <p className="checkout-coupon-success"><Check size={14} /> Đã áp dụng mã {promotionCalculation?.campaignCode || coupon.toUpperCase()}.</p>}
              <div className="checkout-cost"><div><span>Tạm tính</span><b>{money(subtotal)}</b></div>{discount > 0 && <div className="discount"><span>Giảm giá ({promotionCalculation?.campaignCode || coupon.toUpperCase()})</span><b>-{money(discount)}</b></div>}<div><span>Phí vận chuyển</span><b>{money(shipping)}</b></div><div className="checkout-total"><span>Tổng thanh toán</span><b>{money(total)}</b></div></div>
              {notice && <div className="checkout-notice"><CircleAlert size={18} /> {notice}</div>}
              <button className="checkout-pay" type="submit" disabled={submitting}><LockKeyhole size={18} /> {submitting ? "Đang khởi tạo đơn hàng..." : `Xác nhận & thanh toán · ${money(total)}`}</button>
              <div className="checkout-protection"><ShieldCheck size={19} /><p><b>Mua sắm an tâm</b><br />Đơn hàng được bảo vệ và thông tin thanh toán được mã hoá.</p></div>
            </section>
            <p className="checkout-help"><Truck size={16} /> Giao hàng tiêu chuẩn trong 2–5 ngày làm việc.</p>
          </aside>
        </form>
      </section>
    </main>
  );
}
