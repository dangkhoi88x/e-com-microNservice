import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const getCategories = async () => {
  const response = await httpClient.get(API.CATEGORIES);
  return response.data?.data || [];
};
