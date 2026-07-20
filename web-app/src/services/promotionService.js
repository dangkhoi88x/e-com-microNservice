import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const getPromotions = async (status) => {
  const config = {
    params: status && status !== "ALL" ? { status } : {},
  };

  try {
    const response = await httpClient.get(API.PROMOTIONS, config);
    return response.data?.data || [];
  } catch (error) {
    // Eureka/Gateway can briefly have no available promotion-service instance
    // immediately after a container/service restart. Retry this read once so a
    // manual Refresh does not expose that short-lived discovery race to admins.
    const retryable = !error.response || [502, 503, 504].includes(error.response.status);
    if (!retryable) throw error;

    await new Promise((resolve) => setTimeout(resolve, 600));
    const response = await httpClient.get(API.PROMOTIONS, config);
    return response.data?.data || [];
  }
};

export const createPromotion = async (payload) => {
  const response = await httpClient.post(API.PROMOTIONS, payload);
  return response.data?.data;
};

export const updatePromotion = async (id, payload) => {
  const response = await httpClient.put(`${API.PROMOTIONS}/${id}`, payload);
  return response.data?.data;
};

export const deletePromotion = async (id) => {
  await httpClient.delete(`${API.PROMOTIONS}/${id}`);
};
