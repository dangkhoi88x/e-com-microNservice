import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

const defaultPage = (params = {}) => ({
  content: [],
  currentPage: 1,
  pageSize: params.size || 10,
  totalPages: 0,
  totalElements: 0,
});

export const getMyOrders = async (params = {}) => {
  const response = await httpClient.get(`${API.ORDERS}/my-orders`, {
    params,
  });

  return response.data?.data || defaultPage(params);
};

export const getAllOrders = async (params = {}) => {
  const response = await httpClient.get(API.ORDERS, {
    params,
  });

  return response.data?.data || defaultPage(params);
};

export const getOrdersByPromotionCode = async (promotionCode, params = {}) => {
  const response = await httpClient.get(`${API.ORDERS}/by-promotion`, {
    params: { ...params, promotionCode },
  });
  return response.data?.data || defaultPage(params);
};

export const getOrderDetail = async (id) => {
  const response = await httpClient.get(`${API.ORDERS}/${id}`);
  return response.data?.data;
};

export const cancelOrder = async (id) => {
  const response = await httpClient.put(`${API.ORDERS}/${id}/cancel`);
  return response.data?.data;
};

export const updateOrderStatus = async (id, status) => {
  const response = await httpClient.put(`${API.ORDERS}/${id}/status`, null, {
    params: { status },
  });
  return response.data?.data;
};

export const getSellerOrders = async (params = {}) => {
  const response = await httpClient.get(`${API.ORDERS}/seller`, { params });
  return response.data?.data || defaultPage(params);
};

export const updateSellerOrderStatus = async (id, status) => {
  const response = await httpClient.put(`${API.ORDERS}/seller/${id}/status`, null, { params: { status } });
  return response.data?.data;
};
export const getSellerOrderDetail = async (id) => (await httpClient.get(`${API.ORDERS}/seller/${id}`)).data?.data;
export const getSellerAnalytics = async (from, to) => (await httpClient.get(`${API.ORDERS}/seller/analytics`, { params: { from: from.toISOString(), to: to.toISOString() } })).data?.data;

export const checkoutOrder = async ({ shippingAddress, campaignCode }) => {
  const response = await httpClient.post(`${API.ORDERS}/checkout`, {
    shippingAddress,
    campaignCode: campaignCode?.trim() || null,
  });

  return response.data?.data;
};

export const createOrder = async ({ shippingAddress, items }) => {
  const response = await httpClient.post(API.ORDERS, {
    shippingAddress,
    items,
  });

  return response.data?.data;
};
