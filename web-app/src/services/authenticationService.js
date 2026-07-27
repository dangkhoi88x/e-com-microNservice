import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import {
  getToken,
  removeToken,
  setToken,
} from "./localStorageService";

let restorePromise = null;

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

export const requestPasswordReset = async (email) => {
  const response = await httpClient.post(API.PASSWORD_RESET_REQUEST, { email });
  return response.data;
};

export const confirmPasswordReset = async ({ email, otp, newPassword, confirmPassword }) => {
  const response = await httpClient.post(API.PASSWORD_RESET_CONFIRM, { email, otp, newPassword, confirmPassword });
  return response.data;
};

export const restoreSession = () => {
  if (!restorePromise) {
    restorePromise = httpClient.post(API.REFRESH_TOKEN)
      .then((response) => {
        const accessToken = response.data?.data?.accessToken;
        if (!accessToken) throw new Error("Missing refreshed access token");
        setToken(accessToken);
        return true;
      })
      .catch(() => {
        removeToken();
        return false;
      })
      .finally(() => { restorePromise = null; });
  }
  return restorePromise;
};

export const logout = async () => {
  try {
    await httpClient.post(API.LOGOUT);
  } finally {
    removeToken();
  }
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

export const hasAdminRole = () => hasAnyRole("ROLE_ADMIN");
export const hasSellerRole = () => hasAnyRole("ROLE_SELLER");
export const hasUserRole = () => hasAnyRole("ROLE_USER");

export const getRoleHomePath = () => {
  if (hasAdminRole()) return "/admin";
  if (hasAnyRole("ROLE_SHIPPER")) return "/shipper";
  if (hasSellerRole()) return "/seller";
  return "/shop";
};
