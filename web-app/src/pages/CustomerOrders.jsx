import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Check, ChevronRight, CircleAlert, ClipboardList, CreditCard, MapPin, Package, RotateCcw, ShoppingBag, Truck, X, XCircle } from "lucide-react";
import { cancelOrder, getMyOrders, getOrderDetail } from "../services/orderService";
import { createPayment } from "../services/paymentService";
import { addCartItem } from "../services/cartService";
import "./CustomerOrders.css";

const tabs = [{ key: "ALL", label: "Tất cả" }, { key: "PENDING_PAYMENT", label: "Chờ thanh toán" }, { key: "CONFIRMED", label: "Xác nhận" }, { key: "SHIPPING", label: "Đang giao" }, { key: "COMPLETED", label: "Hoàn tất" }, { key: "CANCELLED", label: "Đã huỷ" }];
const statusInfo = { PENDING: ["Đang xử lý", "pending"], PENDING_PAYMENT: ["Chờ thanh toán", "payment"], INVENTORY_FAILED: ["Tạm hết hàng", "failed"], CONFIRMED: ["Đã xác nhận", "confirmed"], SHIPPING: ["Đang giao hàng", "shipping"], COMPLETED: ["Hoàn tất", "completed"], CANCELLED: ["Đã huỷ", "cancelled"] };
const money = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value || 0);
const dateTime = (value) => value ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "—";
const image = (item) => item.imageUrl || item.productImageUrl || "https://placehold.co/120x120/e7f2f8/3b82c4?text=Nova";

export default function CustomerOrders() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [active, setActive] = useState("ALL");
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState("");
  const load = () => { setLoading(true); getMyOrders({ page: 1, size: 50 }).then((data) => setOrders(data.content || [])).catch(() => setNotice("Không thể tải đơn hàng. Vui lòng đăng nhập lại.")).finally(() => setLoading(false)); };
  useEffect(() => {
    load();
    const refreshInterval = window.setInterval(load, 10_000);
    return () => window.clearInterval(refreshInterval);
  }, []);
  const shown = useMemo(() => active === "ALL" ? orders : orders.filter((order) => order.status === active), [orders, active]);
  const openDetail = async (id) => { try { setDetail(await getOrderDetail(id)); } catch { setNotice("Không thể tải chi tiết đơn hàng."); } };
  const cancel = async (id) => { if (!window.confirm("Bạn muốn huỷ đơn hàng này?")) return; try { await cancelOrder(id); setNotice("Đơn hàng đã được huỷ."); setDetail(null); load(); } catch (error) { setNotice(error.response?.data?.message || "Không thể huỷ đơn hàng lúc này."); } };
  const repay = async (order) => { try { await createPayment({ orderId: order.id, method: "VNPAY" }); setNotice("Đã tạo yêu cầu thanh toán VNPay. Bạn có thể tiếp tục tại bước thanh toán."); } catch (error) { setNotice(error.response?.data?.message || "Không thể tạo yêu cầu thanh toán lại."); } };
  const rebuy = async (order) => {
    try {
      await Promise.all((order.items || []).map((item) => addCartItem({ productId: item.productId, variantId: item.variantId || null, quantity: item.quantity })));
      navigate("/cart");
    } catch (error) {
      setNotice(error.response?.data?.message || "Không thể thêm lại sản phẩm vào giỏ hàng.");
    }
  };
  const timeline = (order) => { const current = order.status; const steps = [["Đơn hàng đã tạo", "PENDING", ClipboardList], ["Đã xác nhận", "CONFIRMED", Check], ["Đang vận chuyển", "SHIPPING", Truck], ["Giao hàng thành công", "COMPLETED", Package]]; const rank = { PENDING: 0, PENDING_PAYMENT: 0, CONFIRMED: 1, SHIPPING: 2, COMPLETED: 3 }; return steps.map(([label, status, Icon], index) => ({ label, Icon, done: rank[current] >= index, current: current === status || (current === "PENDING_PAYMENT" && index === 0) })); };

  return <main className="my-orders-page"><header className="my-orders-top"><Link to="/shop" className="my-orders-brand"><i>N</i>Nova<span>Shop</span></Link><Link to="/shop"><ShoppingBag size={18} /> Tiếp tục mua sắm</Link></header><section className="my-orders-shell"><div className="my-orders-title"><p>TÀI KHOẢN CỦA TÔI</p><h1>Đơn hàng của tôi</h1><span>Theo dõi mọi đơn hàng và hành trình giao đến bạn.</span></div><nav className="my-orders-tabs">{tabs.map((tab) => <button key={tab.key} className={active === tab.key ? "active" : ""} onClick={() => setActive(tab.key)}>{tab.label}<b>{tab.key === "ALL" ? orders.length : orders.filter((order) => order.status === tab.key).length}</b></button>)}</nav>{notice && <div className="my-orders-notice"><CircleAlert size={18} />{notice}<button onClick={() => setNotice("")}><X size={15} /></button></div>}<section className="my-orders-list">{loading ? <div className="my-orders-empty">Đang tải đơn hàng…</div> : shown.map((order) => { const [label, tone] = statusInfo[order.status] || [order.status, "pending"]; return <article className="my-order-card" key={order.id}><div className="my-order-head"><div><span>ĐƠN HÀNG #{order.orderCode || order.id.slice(0, 8).toUpperCase()}</span><small>{dateTime(order.createdAt)}</small></div><b className={`my-order-status ${tone}`}>{label}</b></div><div className="my-order-products">{(order.items || []).slice(0, 2).map((item) => <div key={`${order.id}-${item.productId}`}><img src={image(item)} alt=""/><p><b>{item.productName}</b><span>×{item.quantity}</span></p><strong>{money(item.subtotal || Number(item.price || 0) * item.quantity)}</strong></div>)}{(order.items || []).length > 2 && <small>+{order.items.length - 2} sản phẩm khác</small>}</div><div className="my-order-foot"><p>Thành tiền <b>{money(order.totalAmount)}</b></p><div><button className="my-order-detail" onClick={() => openDetail(order.id)}>Xem chi tiết <ChevronRight size={16} /></button>{order.status === "PENDING_PAYMENT" && <button className="my-order-pay" onClick={() => repay(order)}><CreditCard size={15} /> Thanh toán lại</button>}{["PENDING", "PENDING_PAYMENT", "CONFIRMED"].includes(order.status) && <button className="my-order-cancel" onClick={() => cancel(order.id)}>Huỷ đơn</button>}{["COMPLETED", "CANCELLED"].includes(order.status) && <button className="my-order-rebuy" onClick={() => rebuy(order)}><RotateCcw size={14} /> Mua lại</button>}</div></div></article>; })}{!loading && !shown.length && <div className="my-orders-empty"><Package size={39}/><h2>Chưa có đơn hàng phù hợp</h2><p>Khám phá sản phẩm mới và bắt đầu mua sắm cùng NovaShop.</p><Link to="/shop">Đi đến cửa hàng</Link></div>}</section></section>{detail && <div className="order-detail-overlay" onMouseDown={() => setDetail(null)}><section className="order-detail-modal" onMouseDown={(event) => event.stopPropagation()}><button className="order-detail-close" onClick={() => setDetail(null)}><X size={20}/></button><p className="order-detail-eyebrow">CHI TIẾT ĐƠN HÀNG</p><h2>#{detail.orderCode || detail.id.slice(0, 8).toUpperCase()}</h2><div className="order-timeline">{detail.status === "CANCELLED" || detail.status === "INVENTORY_FAILED" ? <div className="order-timeline-failed"><XCircle size={20}/>{statusInfo[detail.status]?.[0]}</div> : timeline(detail).map(({ label, Icon, done, current }) => <div className={done ? "done" : ""} key={label}><span className={current ? "current" : ""}><Icon size={15}/></span><p><b>{label}</b><small>{current ? dateTime(detail.createdAt) : done ? "Đã hoàn tất" : "Đang chờ"}</small></p></div>)}</div><div className="order-detail-address"><MapPin size={18}/><p><b>Địa chỉ nhận hàng</b>{detail.shippingAddress}</p></div><div className="order-detail-items">{(detail.items || []).map((item) => <div key={item.productId}><span>{item.productName} <i>×{item.quantity}</i></span><b>{money(item.subtotal || Number(item.price || 0) * item.quantity)}</b></div>)}</div><div className="order-detail-total"><span>Tổng thanh toán</span><b>{money(detail.totalAmount)}</b></div>{detail.status === "PENDING_PAYMENT" && <button className="order-detail-pay" onClick={() => repay(detail)}><CreditCard size={16}/> Thanh toán lại</button>}</section></div>}</main>;
}
