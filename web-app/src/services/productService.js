import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const getProducts = async () => {
  const response = await httpClient.get(API.PRODUCTS);
  return response.data?.data || [];
};
