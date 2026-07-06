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

export const createProduct = async (payload) => {
  const response = await httpClient.post(API.PRODUCTS, payload);
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
