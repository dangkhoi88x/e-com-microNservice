import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const getMyNotifications = async () => {
  const response = await httpClient.get(API.MY_NOTIFICATIONS);
  return response.data?.data || [];
};
