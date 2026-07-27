import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ArrowLeft, CheckCircle2, Clock3, Eye, EyeOff, KeyRound, LockKeyhole, Mail, ShieldCheck, ShoppingBag } from "lucide-react";
import {
  confirmPasswordReset,
  isAuthenticated,
  login,
  register,
  requestPasswordReset,
} from "../services/authenticationService";
import "./CustomerAuth.css";

export default function CustomerAuth({ mode }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", password: "", otp: "", newPassword: "", confirmPassword: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [resetStage, setResetStage] = useState(null);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [resendIn, setResendIn] = useState(0);
  const redirect = new URLSearchParams(location.search).get("redirect") || "/shop/account";
  const isLogin = mode === "login";

  useEffect(() => {
    if (isAuthenticated()) navigate(redirect, { replace: true });
  }, [navigate, redirect]);

  useEffect(() => {
    if (resendIn <= 0) return undefined;
    const timer = window.setInterval(() => setResendIn((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [resendIn]);

  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  const errorMessage = (requestError, fallback) => requestError.response?.data?.message || fallback;

  const sendResetCode = async () => {
    setSubmitting(true);
    setError("");
    setNotice("");
    try {
      const response = await requestPasswordReset(form.email.trim());
      setResetStage("confirm");
      setResendIn(60);
      setNotice(response?.message || "Nếu email tồn tại, mã xác nhận đã được gửi.");
    } catch (requestError) {
      setError(errorMessage(requestError, "Không thể gửi mã xác nhận. Vui lòng thử lại."));
    } finally {
      setSubmitting(false);
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    if (resetStage === "email") {
      await sendResetCode();
      return;
    }

    setSubmitting(true);
    setError("");
    setNotice("");
    try {
      if (resetStage === "confirm") {
        if (form.newPassword !== form.confirmPassword) throw new Error("Mật khẩu xác nhận không khớp.");
        await confirmPasswordReset({
          email: form.email.trim(),
          otp: form.otp,
          newPassword: form.newPassword,
          confirmPassword: form.confirmPassword,
        });
        setResetStage("done");
        return;
      }

      if (isLogin) {
        await login(form.email, form.password);
        navigate(redirect, { replace: true });
      } else {
        await register({
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim(),
          email: form.email.trim(),
          password: form.password,
        });
        navigate(`/shop/login?redirect=${encodeURIComponent(redirect)}&registered=1`, { replace: true });
      }
    } catch (requestError) {
      setError(requestError.response ? errorMessage(requestError, "Không thể xử lý yêu cầu.") : requestError.message);
    } finally {
      setSubmitting(false);
    }
  };

  const leaveReset = () => {
    setResetStage(null);
    setError("");
    setNotice("");
    setForm((current) => ({ ...current, otp: "", newPassword: "", confirmPassword: "" }));
  };

  const resetTitle = resetStage === "done"
    ? "Mật khẩu đã được cập nhật"
    : resetStage === "confirm"
      ? "Nhập mã xác nhận"
      : "Quên mật khẩu?";

  return (
    <main className="customer-auth-page">
      <section className="customer-auth-showcase">
        <Link to="/shop" className="customer-auth-brand"><i>N</i>Nova<span>Shop</span></Link>
        <div>
          <p>MUA SẮM DỄ DÀNG HƠN</p>
          <h1>Mọi món bạn yêu thích, trong một trải nghiệm liền mạch.</h1>
          <span>Đăng nhập để lưu giỏ hàng, theo dõi đơn và thanh toán an toàn.</span>
        </div>
        <div className="customer-auth-benefits">
          <span><ShieldCheck size={18}/> Thanh toán bảo mật</span>
          <span><ShoppingBag size={18}/> Theo dõi đơn hàng</span>
        </div>
      </section>

      <section className="customer-auth-panel">
        <Link to="/shop" className="customer-auth-back"><ArrowLeft size={17}/> Quay lại cửa hàng</Link>
        <div className="customer-auth-form">
          <p>{resetStage ? "KHÔI PHỤC TÀI KHOẢN" : isLogin ? "CHÀO MỪNG TRỞ LẠI" : "TẠO TÀI KHOẢN"}</p>
          <h2>{resetStage ? resetTitle : isLogin ? "Đăng nhập NovaShop" : "Trở thành thành viên"}</h2>
          <span>
            {resetStage === "email" && "Nhập email tài khoản để nhận mã OTP gồm 6 chữ số."}
            {resetStage === "confirm" && `Mã có hiệu lực 10 phút. Kiểm tra hộp thư của ${form.email}.`}
            {resetStage === "done" && "Bạn có thể đăng nhập ngay bằng mật khẩu mới."}
            {!resetStage && (isLogin ? "Đăng nhập để tiếp tục hành trình mua sắm." : "Đăng ký nhanh để lưu sản phẩm và theo dõi đơn hàng.")}
          </span>

          {resetStage === "done" ? (
            <div className="customer-auth-reset-done">
              <CheckCircle2 size={42}/>
              <button className="customer-auth-submit" onClick={leaveReset}>Quay lại đăng nhập</button>
            </div>
          ) : (
            <form onSubmit={submit}>
              {!resetStage && !isLogin && (
                <div className="customer-auth-two">
                  <label>Họ<input required name="firstName" value={form.firstName} onChange={update} placeholder="Nguyễn"/></label>
                  <label>Tên<input required name="lastName" value={form.lastName} onChange={update} placeholder="Minh Anh"/></label>
                </div>
              )}

              {resetStage !== "confirm" && (
                <label>Email<div><Mail size={17}/><input required type="email" name="email" value={form.email} onChange={update} placeholder="ban@example.com" autoComplete="email"/></div></label>
              )}

              {!resetStage && (
                <label>Mật khẩu<div><LockKeyhole size={17}/><input required minLength="8" type={showPassword ? "text" : "password"} name="password" value={form.password} onChange={update} placeholder="Tối thiểu 8 ký tự" autoComplete={isLogin ? "current-password" : "new-password"}/><button type="button" onClick={() => setShowPassword((value) => !value)}>{showPassword ? <EyeOff size={17}/> : <Eye size={17}/>}</button></div></label>
              )}

              {resetStage === "confirm" && (
                <>
                  <label>Mã OTP<div><KeyRound size={17}/><input required name="otp" inputMode="numeric" pattern="[0-9]{6}" maxLength="6" value={form.otp} onChange={(event) => setForm((current) => ({ ...current, otp: event.target.value.replace(/\D/g, "") }))} placeholder="000000" className="customer-auth-otp" autoComplete="one-time-code"/></div></label>
                  <label>Mật khẩu mới<div><LockKeyhole size={17}/><input required minLength="8" type={showPassword ? "text" : "password"} name="newPassword" value={form.newPassword} onChange={update} placeholder="Tối thiểu 8 ký tự" autoComplete="new-password"/><button type="button" onClick={() => setShowPassword((value) => !value)}>{showPassword ? <EyeOff size={17}/> : <Eye size={17}/>}</button></div></label>
                  <label>Xác nhận mật khẩu<input required minLength="8" type={showPassword ? "text" : "password"} name="confirmPassword" value={form.confirmPassword} onChange={update} placeholder="Nhập lại mật khẩu mới" autoComplete="new-password"/></label>
                  <button className="customer-auth-resend" type="button" disabled={submitting || resendIn > 0} onClick={sendResetCode}><Clock3 size={14}/>{resendIn > 0 ? `Gửi lại sau ${resendIn}s` : "Gửi lại mã"}</button>
                </>
              )}

              {isLogin && !resetStage && <button className="customer-auth-forgot" type="button" onClick={() => { setResetStage("email"); setError(""); }}>Quên mật khẩu?</button>}
              {error && <div className="customer-auth-error">{error}</div>}
              {notice && <div className="customer-auth-notice">{notice}</div>}
              <button className="customer-auth-submit" disabled={submitting}>{submitting ? "Đang xử lý..." : resetStage === "email" ? "Gửi mã xác nhận" : resetStage === "confirm" ? "Đặt lại mật khẩu" : isLogin ? "Đăng nhập" : "Tạo tài khoản"}</button>
            </form>
          )}

          {resetStage && resetStage !== "done" ? (
            <button className="customer-auth-switch" onClick={leaveReset}>← Quay lại đăng nhập</button>
          ) : !resetStage && (
            <p className="customer-auth-switch">{isLogin ? "Chưa có tài khoản?" : "Đã có tài khoản?"}<Link to={isLogin ? `/shop/register?redirect=${encodeURIComponent(redirect)}` : `/shop/login?redirect=${encodeURIComponent(redirect)}`}>{isLogin ? "Đăng ký" : "Đăng nhập"}</Link></p>
          )}
          <small className="customer-auth-refresh">Phiên đăng nhập được tự động làm mới an toàn khi bạn tiếp tục mua sắm.</small>
        </div>
      </section>
    </main>
  );
}
