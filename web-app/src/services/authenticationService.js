import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken, removeToken, setToken } from "./localStorageService";

export const login = async (email, password) => {
  const response = await httpClient.post(API.LOGIN, {
    email,
    password,
  });

  const accessToken = response.data?.data?.accessToken;
  setToken(accessToken);

  return response.data;
};

export const register = async ({ email, password, firstName, lastName }) => {
  const response = await httpClient.post(API.REGISTER, {
    email,
    password,
    firstName,
    lastName,
  });

  return response.data;
};

export const logout = () => {
  removeToken();
};

export const isAuthenticated = () => {
  return Boolean(getToken());
};
