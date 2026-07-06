import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const getMyProfile = async () => {
  const response = await httpClient.get(API.MY_PROFILE);
  return response.data?.data;
};

export const updateMyProfile = async (payload) => {
  const response = await httpClient.put(API.MY_PROFILE, payload);
  return response.data?.data;
};
