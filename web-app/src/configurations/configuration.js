export const CONFIG = {
  API_GATEWAY: "http://localhost:9191",
};

export const API = {
  LOGIN: "/identity/auth/login",
  REFRESH_TOKEN: "/identity/auth/refresh-token",
  REGISTER: "/identity/users",
  MY_PROFILE: "/profile/api/v1/user-profile/me",

  CATEGORIES: "/product/api/v1/categories",
  PRODUCTS: "/product/api/v1/products",
  INVENTORY: "/inventory/api/v1/inventory",
  ORDERS: "/order/api/v1/orders",
  PAYMENTS: "/payment/api/v1/payments",
  WISHLIST: "/api/v1/wishlist",
  MY_NOTIFICATIONS: "/notification/api/v1/notifications/my-notifications",
  ADMIN_NOTIFICATIONS: "/notification/api/v1/notifications/admin",
  SEARCH_PRODUCTS: "/api/v1/search/products",
  SEARCH_AGGREGATIONS: "/api/v1/search/products/aggregations",
};
