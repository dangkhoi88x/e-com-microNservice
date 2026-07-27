import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const createShipper = async (payload) => {
  const response = await httpClient.post(API.ADMIN_SHIPPERS, payload);
  return response.data?.data;
};
