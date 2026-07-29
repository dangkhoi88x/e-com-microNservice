import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

const removeEmptyParams = (params = {}) => {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => {
      return value !== "" && value !== null && value !== undefined;
    }),
  );
};

export const getProducts = async (params = {}) => {
  const response = await httpClient.get(API.PRODUCTS, {
    params: removeEmptyParams(params),
  });
  const data = response.data?.data;

  if (Array.isArray(data)) {
    return {
      content: data,
      currentPage: 1,
      pageSize: data.length,
      totalPages: 1,
      totalElements: data.length,
    };
  }

  return data || {
    content: [],
    currentPage: 1,
    pageSize: params.size || 10,
    totalPages: 0,
    totalElements: 0,
  };
};

export const getAdminProducts = async (params = {}) => {
  const response = await httpClient.get(API.ADMIN_PRODUCTS, {
    params: removeEmptyParams(params),
  });
  return response.data?.data || {
    content: [], currentPage: 1, pageSize: params.size || 10, totalPages: 0, totalElements: 0,
  };
};

export const reviewProduct = async (id, payload) => {
  const response = await httpClient.put(`${API.ADMIN_PRODUCTS}/${id}/review`, payload);
  return response.data?.data;
};

export const createProduct = async (payload) => {
  const response = await httpClient.post(API.PRODUCTS, payload);
  return response.data?.data;
};

export const getMySellerProducts = async (params = {}) => {
  const response = await httpClient.get(API.SELLER_PRODUCTS, { params });
  return response.data?.data || { content: [], currentPage: 1, totalPages: 0, totalElements: 0 };
};

export const getSellerProductById = async (id) => {
  const response = await httpClient.get(`${API.SELLER_PRODUCTS}/${id}`);
  return response.data?.data;
};

export const createSellerProduct = async (payload) => {
  const response = await httpClient.post(API.SELLER_PRODUCTS, payload);
  return response.data?.data;
};

export const updateSellerProduct = async (id, payload) => {
  const response = await httpClient.put(`${API.SELLER_PRODUCTS}/${id}`, payload);
  return response.data?.data;
};

export const addSellerProductVariant = async (id, payload) => {
  const response = await httpClient.post(`${API.SELLER_PRODUCTS}/${id}/variants`, payload);
  return response.data?.data;
};

export const submitSellerProduct = async (id) => {
  const response = await httpClient.post(`${API.SELLER_PRODUCTS}/${id}/submit`);
  return response.data?.data;
};

export const updateSellerProductQuantity = async (id, quantity) => {
  const response = await httpClient.patch(`${API.SELLER_PRODUCTS}/${id}/quantity`, { quantity });
  return response.data?.data;
};

export const updateSellerProductStatus = async (id, status) => {
  const response = await httpClient.patch(`${API.SELLER_PRODUCTS}/${id}/status`, { status });
  return response.data?.data;
};

export const deleteSellerProduct = async (id) => {
  await httpClient.delete(`${API.SELLER_PRODUCTS}/${id}`);
};

export const getProductById = async (id) => {
  const response = await httpClient.get(`${API.PRODUCTS}/${id}`);
  return response.data?.data;
};

export const updateProduct = async (id, payload) => {
  const response = await httpClient.put(`${API.PRODUCTS}/${id}`, payload);
  return response.data?.data;
};

export const deleteProduct = async (id) => {
  await httpClient.delete(`${API.PRODUCTS}/${id}`);
};

export const searchProducts = async (params = {}) => {
  const response = await httpClient.get(API.SEARCH_PRODUCTS, {
    params: removeEmptyParams(params),
  });

  return response.data?.data || {
    content: [],
    currentPage: 1,
    pageSize: params.size || 20,
    totalPages: 0,
    totalElements: 0,
  };
};

export const getProductAggregations = async (params = {}) => {
  const response = await httpClient.get(API.SEARCH_AGGREGATIONS, {
    params: removeEmptyParams(params),
  });

  return (
    response.data?.data || {
      categories: [],
      priceStats: { count: 0 },
      priceRanges: [],
    }
  );
};

export const searchProductSuggestions = async (query, size = 8) => {
  if (!query || query.trim().length < 2) return [];
  const response = await httpClient.get(API.SEARCH_SUGGESTIONS, { params: { q: query.trim(), size } });
  return response.data?.data || [];
};
