import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Dashboard from "../pages/Dashboard";
import Login from "../pages/Login";
import Register from "../pages/Register";
import Products from "../pages/Products";
import ProductCreate from "../pages/ProductCreate";
import ProductEdit from "../pages/ProductEdit";
import Search from "../pages/Search";
import Orders from "../pages/Orders";
import Payments from "../pages/Payments";
import Categories from "../pages/Categories";
import Profile from "../pages/Profile";
import Notifications from "../pages/Notifications";
import Shop from "../pages/Shop";
import ShopProductDetail from "../pages/ShopProductDetail";
import Checkout from "../pages/Checkout";
import Cart from "../pages/Cart";
import ShopCategory from "../pages/ShopCategory";
import CustomerOrders from "../pages/CustomerOrders";
import MyAccount from "../pages/MyAccount";
import CustomerProfile from "../pages/CustomerProfile";
import CustomerAuth from "../pages/CustomerAuth";
import ShopWishlist from "../pages/ShopWishlist";
import ShopHotDeals from "../pages/ShopHotDeals";
import ShopStoreHeader from "../components/ShopStoreHeader";
import "../components/StorefrontLayout.css";
import { isAuthenticated } from "../services/authenticationService";

function PrivateRoute({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

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
        <Route path="/shop/products/:slug" element={<StorefrontLayout showBack><ShopProductDetail /></StorefrontLayout>} />
        <Route path="/shop/categories/:slug" element={<ShopCategory />} />
        <Route path="/shop/orders" element={<PrivateRoute><StorefrontLayout><CustomerOrders /></StorefrontLayout></PrivateRoute>} />
        <Route path="/shop/account" element={<PrivateRoute><StorefrontLayout><MyAccount /></StorefrontLayout></PrivateRoute>} />
        <Route path="/shop/account/profile" element={<PrivateRoute><CustomerProfile /></PrivateRoute>} />
        <Route path="/checkout" element={<Checkout />} />
        <Route path="/cart" element={<Cart />} />

        <Route
          path="/"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />

        <Route
          path="/dashboard"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />

        <Route
          path="/products"
          element={
            <PrivateRoute>
              <Products />
            </PrivateRoute>
          }
        />

        <Route
          path="/products/new"
          element={
            <PrivateRoute>
              <ProductCreate />
            </PrivateRoute>
          }
        />

        <Route
          path="/products/:id/edit"
          element={
            <PrivateRoute>
              <ProductEdit />
            </PrivateRoute>
          }
        />

        <Route
          path="/categories"
          element={
            <PrivateRoute>
              <Categories />
            </PrivateRoute>
          }
        />

        <Route
          path="/search"
          element={
            <PrivateRoute>
              <Search />
            </PrivateRoute>
          }
        />

        <Route
          path="/orders"
          element={
            <PrivateRoute>
              <Orders />
            </PrivateRoute>
          }
        />

        <Route
          path="/payments"
          element={
            <PrivateRoute>
              <Payments />
            </PrivateRoute>
          }
        />

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
