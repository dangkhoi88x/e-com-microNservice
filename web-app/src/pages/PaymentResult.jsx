import {
  ArrowRight,
  CircleCheckBig,
  CircleX,
  Clock3,
  LoaderCircle,
  ReceiptText,
  RotateCcw,
  ShieldCheck,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  createStripeCheckout,
  getPaymentDetail,
  reconcileStripePayment,
} from "../services/paymentService";
import "./PaymentResult.css";

const money = (value) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value || 0));

const resultContent = {
  SUCCESS: {
    icon: CircleCheckBig,
    tone: "success",
    eyebrow: "Thanh toán thành công",
    title: "Đơn hàng của bạn đã được xác nhận",
    description: "Stripe đã xác nhận giao dịch. NovaShop đang chuyển đơn hàng sang bước xử lý tiếp theo.",
  },
  CANCELLED: {
    icon: CircleX,
    tone: "cancel",
    eyebrow: "Thanh toán đã hủy",
    title: "Giao dịch chưa được hoàn tất",
    description: "Bạn đã rời trang Stripe trước khi thanh toán. Payment vẫn được giữ để hệ thống không ghi nhận nhầm giao dịch.",
  },
  FAILED: {
    icon: CircleX,
    tone: "failed",
    eyebrow: "Thanh toán thất bại",
    title: "Stripe chưa thể xử lý giao dịch",
    description: "Vui lòng kiểm tra phương thức thanh toán hoặc thử lại sau.",
  },
  PENDING: {
    icon: Clock3,
    tone: "pending",
    eyebrow: "Đang xác nhận thanh toán",
    title: "Chúng tôi đang chờ phản hồi từ Stripe",
    description: "Bạn có thể giữ nguyên trang này trong vài giây. Trạng thái sẽ tự cập nhật khi webhook Stripe được xử lý.",
  },
};

export default function PaymentResult() {
  const [searchParams] = useSearchParams();
  const paymentId = searchParams.get("paymentId") || "";
  const redirectStatus = searchParams.get("status");
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(Boolean(paymentId));
  const [retrying, setRetrying] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!paymentId) {
      setLoading(false);
      setError("Không tìm thấy mã thanh toán.");
      return undefined;
    }

    let cancelled = false;
    let attempts = 0;
    let timeoutId;
    let reconciliationAttempted = false;

    const loadPayment = async () => {
      try {
        if (redirectStatus === "success" && !reconciliationAttempted) {
          reconciliationAttempted = true;
          try {
            await reconcileStripePayment(paymentId);
          } catch {
            // The verified webhook remains the primary path. If it arrives a
            // little later, polling below will still update this screen.
          }
        }

        const data = await getPaymentDetail(paymentId);
        if (cancelled) return;
        setPayment(data);
        setError("");
        attempts += 1;

        if (redirectStatus === "success" && data?.status === "PENDING" && attempts < 12) {
          timeoutId = window.setTimeout(loadPayment, 2000);
        } else {
          setLoading(false);
        }
      } catch (requestError) {
        if (cancelled) return;
        setLoading(false);
        setError(requestError.response?.data?.message || "Không thể tải trạng thái thanh toán.");
      }
    };

    loadPayment();
    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    };
  }, [paymentId, redirectStatus]);

  const displayedStatus =
    redirectStatus === "cancel" && (!payment || payment.status === "PENDING")
      ? "CANCELLED"
      : payment?.status || "PENDING";
  const content = resultContent[displayedStatus] || resultContent.PENDING;
  const Icon = content.icon;
  const canRetryStripe =
    payment?.method === "STRIPE" &&
    payment?.status === "PENDING" &&
    redirectStatus === "cancel";

  const retryStripeCheckout = async () => {
    setRetrying(true);
    setError("");
    try {
      const checkout = await createStripeCheckout(payment.id);
      if (!checkout?.checkoutUrl) {
        throw new Error("Stripe không trả về đường dẫn thanh toán.");
      }
      window.location.assign(checkout.checkoutUrl);
    } catch (retryError) {
      setRetrying(false);
      setError(
        retryError.response?.data?.message ||
        retryError.message ||
        "Không thể mở lại trang thanh toán Stripe.",
      );
    }
  };

  return (
    <main className={`payment-result-page is-${content.tone}`}>
      <section className="payment-result-card">
        <div className="payment-result-brand">
          <span>N</span>
          <strong>NovaShop</strong>
          <em><ShieldCheck size={15} /> Stripe Checkout</em>
        </div>

        <div className="payment-result-icon">
          {loading ? <LoaderCircle className="payment-result-spinner" /> : <Icon />}
        </div>

        <p className="payment-result-eyebrow">{content.eyebrow}</p>
        <h1>{content.title}</h1>
        <p className="payment-result-description">{error || content.description}</p>

        {payment && (
          <dl className="payment-result-details">
            <div>
              <dt>Mã giao dịch</dt>
              <dd>{payment.transactionCode}</dd>
            </div>
            <div>
              <dt>Phương thức</dt>
              <dd>{payment.method}</dd>
            </div>
            <div>
              <dt>Tổng thanh toán</dt>
              <dd>{money(payment.amount)}</dd>
            </div>
            <div>
              <dt>Trạng thái</dt>
              <dd>{displayedStatus}</dd>
            </div>
          </dl>
        )}

        <div className="payment-result-actions">
          {canRetryStripe && (
            <button
              type="button"
              className="payment-result-primary payment-result-retry"
              disabled={retrying}
              onClick={retryStripeCheckout}
            >
              {retrying
                ? <><LoaderCircle className="payment-result-spinner" size={18} /> Đang mở Stripe...</>
                : <><RotateCcw size={18} /> Thử thanh toán lại <ArrowRight size={17} /></>}
            </button>
          )}
          <Link
            to="/shop/orders"
            className={canRetryStripe ? "payment-result-secondary" : "payment-result-primary"}
          >
            <ReceiptText size={18} /> Xem đơn hàng <ArrowRight size={17} />
          </Link>
          <Link to="/shop" className="payment-result-secondary">Tiếp tục mua sắm</Link>
        </div>

        <small>Trạng thái thanh toán được xác nhận trực tiếp từ webhook Stripe.</small>
      </section>
    </main>
  );
}
