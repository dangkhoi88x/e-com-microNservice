import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Login from "../pages/Login";
import Register from "../pages/Register";
import Products from "../pages/Products";
import ProductCreate from "../pages/ProductCreate";
import Search from "../pages/Search";
import Categories from "../pages/Categories";
import Profile from "../pages/Profile";
import Notifications from "../pages/Notifications";
import { isAuthenticated } from "../services/authenticationService";

function PrivateRoute({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route
          path="/"
          element={
            <PrivateRoute>
              <Products />
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
