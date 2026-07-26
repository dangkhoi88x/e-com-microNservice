import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Dashboard from "../pages/Dashboard";
import AdminAnalytics from "../pages/AdminAnalytics";
import Login from "../pages/Login";
import Register from "../pages/Register";
import Products from "../pages/Products";
import ProductCreate from "../pages/ProductCreate";
import ProductEdit from "../pages/ProductEdit";
import SellerProducts from "../pages/SellerProducts";
import SellerOrders from "../pages/SellerOrders";
import SellerOrderDetail from "../pages/SellerOrderDetail";
import SellerAnalytics from "../pages/SellerAnalytics";
import SellerDashboard from "../pages/SellerDashboard";
import SellerRegister from "../pages/SellerRegister";
import SellerShop from "../pages/SellerShop";
import AdminSellers from "../pages/AdminSellers";
import Search from "../pages/Search";
import Orders from "../pages/Orders";
import Payments from "../pages/Payments";
import Categories from "../pages/Categories";
import Profile from "../pages/Profile";
import Notifications from "../pages/Notifications";
import Promotions from "../pages/Promotions";
import FlashDeals from "../pages/FlashDeals";
import Shop from "../pages/Shop";
import ShopProductDetail from "../pages/ShopProductDetail";
import Checkout from "../pages/Checkout";
import PaymentResult from "../pages/PaymentResult";
import Cart from "../pages/Cart";
import ShopCategory from "../pages/ShopCategory";
import CustomerOrders from "../pages/CustomerOrders";
import MyAccount from "../pages/MyAccount";
import CustomerProfile from "../pages/CustomerProfile";
import CustomerAuth from "../pages/CustomerAuth";
import ShopWishlist from "../pages/ShopWishlist";
import ShopHotDeals from "../pages/ShopHotDeals";
import ShopBestDeals from "../pages/ShopBestDeals";
import ShopSearch from "../pages/ShopSearch";
import Shipments from "../pages/Shipments";
import ShopStoreHeader from "../components/ShopStoreHeader";
import "../components/StorefrontLayout.css";
import { getRoleHomePath, hasAnyRole, isAuthenticated } from "../services/authenticationService";

function PrivateRoute({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

function RoleRoute({ roles, children }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!hasAnyRole(...roles)) return <Navigate to={getRoleHomePath()} replace />;
  return children;
}

function SellerRegistrationRoute({ children }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (hasAnyRole("ROLE_SELLER", "SELLER")) return <Navigate to="/seller" replace />;
  if (hasAnyRole("ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN")) return <Navigate to="/admin" replace />;
  return children;
}

function StorefrontLayout({ children, showBack = false }) {
  return <div className="storefront-layout"><ShopStoreHeader showBack={showBack} />{children}</div>;
}

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/shop" element={<Shop />} />
        <Route path="/shop/login" element={<CustomerAuth mode="login" />} />
        <Route path="/shop/register" element={<CustomerAuth mode="register" />} />
        <Route path="/shop/wishlist" element={<StorefrontLayout><ShopWishlist /></StorefrontLayout>} />
        <Route path="/shop/hot-deals" element={<StorefrontLayout><ShopHotDeals /></StorefrontLayout>} />
        <Route path="/shop/best-deals" element={<ShopBestDeals />} />
        <Route path="/shop/search" element={<ShopSearch />} />
        <Route path="/shop/products/:slug" element={<StorefrontLayout showBack><ShopProductDetail /></StorefrontLayout>} />
        <Route path="/shop/categories" element={<StorefrontLayout><ShopCategory /></StorefrontLayout>} />
        <Route path="/shop/categories/:slug" element={<StorefrontLayout><ShopCategory /></StorefrontLayout>} />
        <Route path="/shop/orders" element={<PrivateRoute><StorefrontLayout><CustomerOrders /></StorefrontLayout></PrivateRoute>} />
        <Route path="/shop/account" element={<PrivateRoute><StorefrontLayout><MyAccount /></StorefrontLayout></PrivateRoute>} />
        <Route path="/shop/account/profile" element={<PrivateRoute><CustomerProfile /></PrivateRoute>} />
        <Route path="/checkout" element={<Checkout />} />
        <Route path="/shop/payment-result" element={<PrivateRoute><PaymentResult /></PrivateRoute>} />
        <Route path="/cart" element={<Cart />} />

        <Route path="/" element={<PrivateRoute><Navigate to={getRoleHomePath()} replace /></PrivateRoute>} />
        <Route path="/admin" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><Dashboard /></RoleRoute>} />
        <Route path="/admin/analytics" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><AdminAnalytics /></RoleRoute>} />
        <Route path="/admin/products" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><Products /></RoleRoute>} />
        <Route path="/admin/categories" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><Categories /></RoleRoute>} />
        <Route path="/admin/search" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><Search /></RoleRoute>} />
        <Route path="/admin/orders" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><Orders /></RoleRoute>} />
        <Route path="/admin/payments" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><Payments /></RoleRoute>} />
        <Route path="/admin/promotions" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><Promotions /></RoleRoute>} />
        <Route path="/admin/flash-deals" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><FlashDeals /></RoleRoute>} />
        <Route path="/admin/shipments" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><Shipments /></RoleRoute>} />
        <Route path="/admin/sellers" element={<RoleRoute roles={["ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN"]}><AdminSellers /></RoleRoute>} />

        <Route path="/seller" element={<RoleRoute roles={["ROLE_SELLER", "SELLER"]}><SellerDashboard /></RoleRoute>} />
        <Route path="/seller/register" element={<SellerRegistrationRoute><SellerRegister /></SellerRegistrationRoute>} />
        <Route path="/seller/products" element={<RoleRoute roles={["ROLE_SELLER", "SELLER"]}><SellerProducts /></RoleRoute>} />
        <Route path="/seller/orders" element={<RoleRoute roles={["ROLE_SELLER", "SELLER"]}><SellerOrders /></RoleRoute>} />
        <Route path="/seller/orders/:id" element={<RoleRoute roles={["ROLE_SELLER", "SELLER"]}><SellerOrderDetail /></RoleRoute>} />
        <Route path="/seller/analytics" element={<RoleRoute roles={["ROLE_SELLER", "SELLER"]}><SellerAnalytics /></RoleRoute>} />
        <Route path="/seller/promotions" element={<RoleRoute roles={["ROLE_SELLER", "SELLER"]}><FlashDeals sellerMode /></RoleRoute>} />
        <Route path="/seller/products/new" element={<RoleRoute roles={["ROLE_SELLER", "SELLER"]}><ProductCreate /></RoleRoute>} />
        <Route path="/seller/products/:id/edit" element={<RoleRoute roles={["ROLE_SELLER", "SELLER"]}><ProductEdit /></RoleRoute>} />
        <Route path="/seller/shop" element={<RoleRoute roles={["ROLE_SELLER", "SELLER"]}><SellerShop /></RoleRoute>} />

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
    </BrowserRouter>
  );
}
