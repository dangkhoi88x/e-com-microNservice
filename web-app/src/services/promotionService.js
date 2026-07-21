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

export const getActivePromotions = async () => {
  const response = await httpClient.get(API.PROMOTIONS_ACTIVE);
  return response.data?.data || [];
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

export const previewPromotion = async (campaignCode, subtotalAmount) => {
  const response = await httpClient.post(API.PROMOTION_PREVIEW, {
    campaignCode: campaignCode.trim(),
    subtotalAmount,
  });
  return response.data?.data;
};

export const getClaimedPromotions = async () => {
  const response = await httpClient.get(API.PROMOTION_CLAIMS);
  return response.data?.data || [];
};

export const claimPromotion = async (campaignId) => {
  const response = await httpClient.post(`${API.PROMOTION_CLAIMS}/${campaignId}`);
  return response.data?.data;
};

export const getFlashDeals = async (status) => (await httpClient.get(API.FLASH_DEALS, { params: status ? { status } : {} })).data?.data || [];
export const getLiveFlashDeals = async () => (await httpClient.get(API.FLASH_DEALS_LIVE)).data?.data || [];
export const getActiveProductSales = async () => (await httpClient.get(API.FLASH_DEALS_ACTIVE)).data?.data || [];
export const getLiveLongTermSales = async () => (await httpClient.get(API.LONG_TERM_SALES_LIVE)).data?.data || [];
export const getUpcomingFlashDeals = async () => (await httpClient.get(API.FLASH_DEALS_UPCOMING)).data?.data || [];
export const getFlashDealNotificationSubscriptions = async () => (await httpClient.get(API.FLASH_DEAL_NOTIFICATION_SUBSCRIPTIONS)).data?.data || [];
export const subscribeFlashDealNotification = async (flashDealId) => { await httpClient.post(`${API.FLASH_DEALS}/${flashDealId}/notifications`); };
export const createFlashDeal = async (payload) => (await httpClient.post(API.FLASH_DEALS, payload)).data?.data;
export const updateFlashDeal = async (id, payload) => (await httpClient.put(`${API.FLASH_DEALS}/${id}`, payload)).data?.data;
export const deleteFlashDeal = async (id) => { await httpClient.delete(`${API.FLASH_DEALS}/${id}`); };
