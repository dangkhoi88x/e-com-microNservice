import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const confirmInventory = async (orderId) => {
  await httpClient.post(`${API.INVENTORY}/confirm`, { orderId });
};

export const releaseInventory = async (orderId) => {
  await httpClient.post(`${API.INVENTORY}/release`, { orderId });
};
