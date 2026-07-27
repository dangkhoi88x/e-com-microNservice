import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));
const isTemporaryFailure = (error) =>
  !error.response || (error.response.status >= 500 && error.response.status < 600);

const getWithRetry = async (url, config) => {
  const maxAttempts = 3;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      const response = await httpClient.get(url, config);
      return response.data?.data || [];
    } catch (error) {
      if (!isTemporaryFailure(error) || attempt === maxAttempts) throw error;
      await wait(400 * attempt);
    }
  }
};

export const getPromotions = async (status) => {
  const config = {
    params: status && status !== "ALL" ? { status } : {},
  };
  return getWithRetry(API.PROMOTIONS, config);
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
export const getFlashDealDetail = async (id) => (await httpClient.get(`${API.FLASH_DEALS}/${id}/detail`)).data?.data;
export const getLiveFlashDeals = async () => getWithRetry(API.FLASH_DEALS_LIVE);
export const getActiveProductSales = async () => getWithRetry(API.FLASH_DEALS_ACTIVE);
export const getLiveLongTermSales = async () => getWithRetry(API.LONG_TERM_SALES_LIVE);
export const getUpcomingFlashDeals = async () => getWithRetry(API.FLASH_DEALS_UPCOMING);
export const getFlashDealNotificationSubscriptions = async () => (await httpClient.get(API.FLASH_DEAL_NOTIFICATION_SUBSCRIPTIONS)).data?.data || [];
export const subscribeFlashDealNotification = async (flashDealId) => { await httpClient.post(`${API.FLASH_DEALS}/${flashDealId}/notifications`); };
export const createFlashDeal = async (payload) => (await httpClient.post(API.FLASH_DEALS, payload)).data?.data;
export const updateFlashDeal = async (id, payload) => (await httpClient.put(`${API.FLASH_DEALS}/${id}`, payload)).data?.data;
export const deleteFlashDeal = async (id) => { await httpClient.delete(`${API.FLASH_DEALS}/${id}`); };
export const getSellerFlashDeals = async () => (await httpClient.get(`${API.FLASH_DEALS}/seller`)).data?.data || [];
export const getSellerFlashDealDetail = async (id) => (await httpClient.get(`${API.FLASH_DEALS}/seller/${id}/detail`)).data?.data;
export const createSellerFlashDeal = async (payload) => (await httpClient.post(`${API.FLASH_DEALS}/seller`, payload)).data?.data;
export const updateSellerFlashDeal = async (id, payload) => (await httpClient.put(`${API.FLASH_DEALS}/seller/${id}`, payload)).data?.data;
export const deleteSellerFlashDeal = async (id) => { await httpClient.delete(`${API.FLASH_DEALS}/seller/${id}`); };
