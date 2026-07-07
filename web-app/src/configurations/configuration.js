export const CONFIG = {
  API_GATEWAY: "http://localhost:9191",
};

export const API = {
  LOGIN: "/identity/auth/login",
  REGISTER: "/identity/users",
  MY_PROFILE: "/profile/api/v1/user-profile/me",

  CATEGORIES: "/product/api/v1/categories",
  PRODUCTS: "/product/api/v1/products",
  ORDERS: "/order/api/v1/orders",
  PAYMENTS: "/payment/api/v1/payments",
  MY_NOTIFICATIONS: "/notification/api/v1/notifications/my-notifications",
  SEARCH_PRODUCTS: "/api/v1/search/products",
  SEARCH_AGGREGATIONS: "/api/v1/search/products/aggregations",
};
