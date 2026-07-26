import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

const defaultPage = (params = {}) => ({
  content: [],
  currentPage: 1,
  pageSize: params.size || 10,
  totalPages: 0,
  totalElements: 0,
});

export const createPayment = async (request) => {
  const response = await httpClient.post(API.PAYMENTS, request);
  return response.data?.data;
};

export const createStripeCheckout = async (paymentId) => {
  const response = await httpClient.post(
    `${API.PAYMENTS}/${paymentId}/stripe-checkout`,
  );
  return response.data?.data;
};

export const reconcileStripePayment = async (paymentId) => {
  const response = await httpClient.post(
    `${API.PAYMENTS}/${paymentId}/stripe-reconcile`,
  );
  return response.data?.data;
};

export const getMyPayments = async (params = {}) => {
  const response = await httpClient.get(`${API.PAYMENTS}/my-payments`, {
    params,
  });

  return response.data?.data || defaultPage(params);
};

export const getAllPayments = async (params = {}) => {
  const response = await httpClient.get(API.PAYMENTS, {
    params,
  });

  return response.data?.data || defaultPage(params);
};

export const getPaymentDetail = async (id) => {
  const response = await httpClient.get(`${API.PAYMENTS}/${id}`);
  return response.data?.data;
};

export const markPaymentSuccess = async (id) => {
  const response = await httpClient.put(`${API.PAYMENTS}/${id}/success`);
  return response.data?.data;
};

export const markPaymentFailed = async (id) => {
  const response = await httpClient.put(`${API.PAYMENTS}/${id}/failed`);
  return response.data?.data;
};

export const cancelPayment = async (id) => {
  const response = await httpClient.put(`${API.PAYMENTS}/${id}/cancel`);
  return response.data?.data;
};
