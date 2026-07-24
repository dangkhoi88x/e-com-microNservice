import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const createSellerShop = async (payload) => {
  const response = await httpClient.post(API.SELLER_SHOP, payload);
  return response.data?.data;
};

export const getMySellerShop = async () => {
  const response = await httpClient.get("/api/v1/sellers/me");
  return response.data?.data;
};

export const updateMySellerShop = async (payload) => {
  const response = await httpClient.put(API.SELLER_SHOP, payload);
  return response.data?.data;
};

export const resubmitSellerShop = async () => {
  const response = await httpClient.post(`${API.SELLER_SHOP}/resubmit`);
  return response.data?.data;
};

export const getSellerShopsForAdmin = async (params = {}) => {
  const response = await httpClient.get(API.ADMIN_SELLER_SHOPS, { params });
  return response.data?.data || { content: [] };
};

export const getSellerShopForAdmin = async (shopId) => {
  const response = await httpClient.get(`${API.ADMIN_SELLER_SHOPS}/${shopId}`);
  return response.data?.data;
};

export const reviewSellerShop = async (shopId, payload) => {
  const response = await httpClient.put(`${API.ADMIN_SELLER_SHOPS}/${shopId}/review`, payload);
  return response.data?.data;
};
