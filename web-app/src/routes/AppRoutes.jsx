import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { lazy, Suspense } from "react";
import Login from "../pages/Login";
import Register from "../pages/Register";
import SellerRegister from "../pages/SellerRegister";
import Shop from "../pages/Shop";
import ShopProductDetail from "../pages/ShopProductDetail";
import Checkout from "../pages/Checkout";
import ShopCategory from "../pages/ShopCategory";
import CustomerAuth from "../pages/CustomerAuth";
import ShopWishlist from "../pages/ShopWishlist";
import ShopHotDeals from "../pages/ShopHotDeals";
import ShopBestDeals from "../pages/ShopBestDeals";
import ShopSearch from "../pages/ShopSearch";
import ShopStoreHeader from "../components/ShopStoreHeader";
import "../components/StorefrontLayout.css";
import { getRoleHomePath, hasAnyRole, hasSellerRole, hasUserRole, isAuthenticated } from "../services/authenticationService";

const Dashboard = lazy(() => import("../pages/Dashboard"));
const AdminAnalytics = lazy(() => import("../pages/AdminAnalytics"));
const Products = lazy(() => import("../pages/Products"));
const ProductCreate = lazy(() => import("../pages/ProductCreate"));
const ProductEdit = lazy(() => import("../pages/ProductEdit"));
const SellerProducts = lazy(() => import("../pages/SellerProducts"));
const SellerOrders = lazy(() => import("../pages/SellerOrders"));
const SellerOrderDetail = lazy(() => import("../pages/SellerOrderDetail"));
const SellerAnalytics = lazy(() => import("../pages/SellerAnalytics"));
const SellerDashboard = lazy(() => import("../pages/SellerDashboard"));
const SellerShop = lazy(() => import("../pages/SellerShop"));
const AdminSellers = lazy(() => import("../pages/AdminSellers"));
const AdminShippers = lazy(() => import("../pages/AdminShippers"));
const Search = lazy(() => import("../pages/Search"));
const Orders = lazy(() => import("../pages/Orders"));
const Payments = lazy(() => import("../pages/Payments"));
const Categories = lazy(() => import("../pages/Categories"));
const Profile = lazy(() => import("../pages/Profile"));
const Notifications = lazy(() => import("../pages/Notifications"));
const Promotions = lazy(() => import("../pages/Promotions"));
const FlashDeals = lazy(() => import("../pages/FlashDeals"));
const PaymentResult = lazy(() => import("../pages/PaymentResult"));
const Cart = lazy(() => import("../pages/Cart"));
const CustomerOrders = lazy(() => import("../pages/CustomerOrders"));
const MyAccount = lazy(() => import("../pages/MyAccount"));
const CustomerProfile = lazy(() => import("../pages/CustomerProfile"));
const Shipments = lazy(() => import("../pages/Shipments"));
const Forbidden = lazy(() => import("../pages/Forbidden"));

const ADMIN_ROLES = ["ROLE_ADMIN"];
const SELLER_ROLES = ["ROLE_SELLER"];

function PrivateRoute({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

function RoleRoute({ roles, deniedPath, children }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!hasAnyRole(...roles)) return <Navigate to={deniedPath ?? getRoleHomePath()} replace />;
  return children;
}

function SellerRegistrationRoute({ children }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (hasSellerRole()) return <Navigate to="/seller" replace />;
  if (hasAnyRole(...ADMIN_ROLES)) return <Navigate to="/admin" replace />;
  if (hasAnyRole("ROLE_SHIPPER")) return <Navigate to="/shipper" replace />;
  return children;
}

function UserRoute({ children }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!hasUserRole()) return <Navigate to="/forbidden" replace />;
  return children;
}

function StorefrontLayout({ children, showBack = false }) {
  return <div className="storefront-layout"><ShopStoreHeader showBack={showBack} />{children}</div>;
}

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Suspense fallback={<main style={{ padding: 24 }}>Loading...</main>}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/shop" element={<Shop />} />
        <Route path="/shop/login" element={<CustomerAuth mode="login" />} />
        <Route path="/shop/register" element={<CustomerAuth mode="register" />} />
        <Route path="/shop/wishlist" element={<UserRoute><StorefrontLayout><ShopWishlist /></StorefrontLayout></UserRoute>} />
        <Route path="/shop/hot-deals" element={<StorefrontLayout><ShopHotDeals /></StorefrontLayout>} />
        <Route path="/shop/best-deals" element={<ShopBestDeals />} />
        <Route path="/shop/search" element={<ShopSearch />} />
        <Route path="/shop/products/:slug" element={<StorefrontLayout showBack><ShopProductDetail /></StorefrontLayout>} />
        <Route path="/shop/categories" element={<StorefrontLayout><ShopCategory /></StorefrontLayout>} />
        <Route path="/shop/categories/:slug" element={<StorefrontLayout><ShopCategory /></StorefrontLayout>} />
        <Route path="/shop/orders" element={<UserRoute><StorefrontLayout><CustomerOrders /></StorefrontLayout></UserRoute>} />
        <Route path="/shop/account" element={<UserRoute><StorefrontLayout><MyAccount /></StorefrontLayout></UserRoute>} />
        <Route path="/shop/account/profile" element={<UserRoute><CustomerProfile /></UserRoute>} />
        <Route path="/checkout" element={<UserRoute><Checkout /></UserRoute>} />
        <Route path="/shop/payment-result" element={<UserRoute><PaymentResult /></UserRoute>} />
        <Route path="/cart" element={<UserRoute><Cart /></UserRoute>} />
        <Route path="/forbidden" element={<Forbidden />} />

        <Route path="/" element={<PrivateRoute><Navigate to={getRoleHomePath()} replace /></PrivateRoute>} />
        <Route path="/admin" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><Dashboard /></RoleRoute>} />
        <Route path="/admin/analytics" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><AdminAnalytics /></RoleRoute>} />
        <Route path="/admin/products" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><Products /></RoleRoute>} />
        <Route path="/admin/categories" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><Categories /></RoleRoute>} />
        <Route path="/admin/search" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><Search /></RoleRoute>} />
        <Route path="/admin/orders" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><Orders /></RoleRoute>} />
        <Route path="/admin/payments" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><Payments /></RoleRoute>} />
        <Route path="/admin/promotions" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><Promotions /></RoleRoute>} />
        <Route path="/admin/flash-deals" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><FlashDeals /></RoleRoute>} />
        <Route path="/admin/shipments" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><Shipments /></RoleRoute>} />
        <Route path="/admin/sellers" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><AdminSellers /></RoleRoute>} />
        <Route path="/admin/shippers" element={<RoleRoute roles={ADMIN_ROLES} deniedPath="/forbidden"><AdminShippers /></RoleRoute>} />

        <Route path="/shipper" element={<RoleRoute roles={["ROLE_SHIPPER"]} deniedPath="/forbidden"><Shipments shipperMode /></RoleRoute>} />

        <Route path="/seller" element={<RoleRoute roles={SELLER_ROLES} deniedPath="/forbidden"><SellerDashboard /></RoleRoute>} />
        <Route path="/seller/register" element={<SellerRegistrationRoute><SellerRegister /></SellerRegistrationRoute>} />
        <Route path="/seller/products" element={<RoleRoute roles={SELLER_ROLES} deniedPath="/forbidden"><SellerProducts /></RoleRoute>} />
        <Route path="/seller/orders" element={<RoleRoute roles={SELLER_ROLES} deniedPath="/forbidden"><SellerOrders /></RoleRoute>} />
        <Route path="/seller/orders/:id" element={<RoleRoute roles={SELLER_ROLES} deniedPath="/forbidden"><SellerOrderDetail /></RoleRoute>} />
        <Route path="/seller/analytics" element={<RoleRoute roles={SELLER_ROLES} deniedPath="/forbidden"><SellerAnalytics /></RoleRoute>} />
        <Route path="/seller/promotions" element={<RoleRoute roles={SELLER_ROLES} deniedPath="/forbidden"><FlashDeals sellerMode /></RoleRoute>} />
        <Route path="/seller/products/new" element={<RoleRoute roles={SELLER_ROLES} deniedPath="/forbidden"><ProductCreate /></RoleRoute>} />
        <Route path="/seller/products/:id/edit" element={<RoleRoute roles={SELLER_ROLES} deniedPath="/forbidden"><ProductEdit /></RoleRoute>} />
        <Route path="/seller/shop" element={<RoleRoute roles={SELLER_ROLES} deniedPath="/forbidden"><SellerShop /></RoleRoute>} />

        <Route path="/dashboard" element={<Navigate to="/" replace />} />
        <Route path="/seller/dashboard" element={<Navigate to="/seller" replace />} />
        <Route path="/products" element={<Navigate to="/admin/products" replace />} />
        <Route path="/products/new" element={<Navigate to="/admin/products" replace />} />
        <Route path="/products/:id/edit" element={<Navigate to="/admin/products" replace />} />
        <Route path="/categories" element={<Navigate to="/admin/categories" replace />} />
        <Route path="/search" element={<Navigate to="/admin/search" replace />} />
        <Route path="/orders" element={<Navigate to="/admin/orders" replace />} />
        <Route path="/payments" element={<Navigate to="/admin/payments" replace />} />
        <Route path="/promotions" element={<Navigate to="/admin/promotions" replace />} />
        <Route path="/flash-deals" element={<Navigate to="/admin/flash-deals" replace />} />
        <Route path="/shipments" element={<Navigate to="/admin/shipments" replace />} />

        <Route
          path="/profile"
          element={
            <PrivateRoute>
              <Profile />
            </PrivateRoute>
          }
        />

        <Route
          path="/notifications"
          element={
            <PrivateRoute>
              <Notifications />
            </PrivateRoute>
          }
        />
      </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
