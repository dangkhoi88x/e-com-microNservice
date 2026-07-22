import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const getShipments = async ({ status, page = 0, size = 20 } = {}) => {
  const response = await httpClient.get(API.SHIPMENTS, {
    params: {
      page,
      size,
      ...(status && status !== "ALL" ? { status } : {}),
    },
  });
  return response.data?.data || { content: [], totalPages: 0, totalElements: 0, number: page, size };
};

export const assignShipmentCarrier = async (shipmentId, payload) => {
  const response = await httpClient.put(`${API.SHIPMENTS}/${shipmentId}/carrier`, payload);
  return response.data?.data;
};

export const updateShipmentState = async (shipmentId, action, payload = {}) => {
  const response = await httpClient.put(`${API.SHIPMENTS}/${shipmentId}/${action}`, payload);
  return response.data?.data;
};
