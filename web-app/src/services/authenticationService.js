import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import {
  getToken,
  removeToken,
  setRefreshToken,
  setToken,
} from "./localStorageService";

export const login = async (email, password) => {
  const response = await httpClient.post(API.LOGIN, {
    email,
    password,
  });

  const accessToken = response.data?.data?.accessToken;
  const refreshToken = response.data?.data?.refreshToken;
  setToken(accessToken);
  setRefreshToken(refreshToken);

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

export const getCurrentUserRoles = () => {
  const token = getToken();
  if (!token) return [];

  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return Array.isArray(payload.roles) ? payload.roles : [];
  } catch {
    return [];
  }
};

export const hasAnyRole = (...roles) => {
  const currentRoles = getCurrentUserRoles();
  return roles.some((role) => currentRoles.includes(role));
};
