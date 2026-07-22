import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

const emptyPage = { content: [], currentPage: 1, pageSize: 10, totalPages: 0, totalElements: 0 };

export const getProductReviews = async (productId, params = {}) => {
  const response = await httpClient.get(`${API.REVIEWS}/products/${productId}`, { params });
  return response.data?.data || emptyPage;
};

export const getProductReviewSummary = async (productId) => {
  const response = await httpClient.get(`${API.REVIEWS}/products/${productId}/summary`);
  return response.data?.data || {
    productId,
    averageRating: 0,
    reviewCount: 0,
    ratingDistribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
  };
};

export const getMyReviews = async (params = {}) => {
  const response = await httpClient.get(`${API.REVIEWS}/me`, { params });
  return response.data?.data || emptyPage;
};

export const getMyReviewByOrderItem = async (orderItemId) => {
  try {
    const response = await httpClient.get(`${API.REVIEWS}/order-items/${orderItemId}`);
    return response.data?.data || null;
  } catch (error) {
    if (error.response?.status === 404) return null;
    throw error;
  }
};

export const createReview = async (payload) => {
  const response = await httpClient.post(API.REVIEWS, payload);
  return response.data?.data;
};

export const updateReview = async (reviewId, payload) => {
  const response = await httpClient.put(`${API.REVIEWS}/${reviewId}`, payload);
  return response.data?.data;
};
