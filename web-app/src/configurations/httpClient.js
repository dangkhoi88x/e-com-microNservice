import axios from "axios";
import { API, CONFIG } from "./configuration";
import {
  getToken,
  removeToken,
  setToken,
} from "../services/localStorageService.js";

const httpClient = axios.create({
  baseURL: CONFIG.API_GATEWAY,
  timeout: 30000,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

let refreshPromise = null;

httpClient.interceptors.request.use((config) => {
  const token = getToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;  //mọi API cần token sẽ tự có Bearer token.
  }

  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const isRefreshRequest = originalRequest?.url?.includes(API.REFRESH_TOKEN);

    if (status !== 401 || originalRequest?._retry || isRefreshRequest) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      if (!refreshPromise) {
        refreshPromise = axios.post(
          `${CONFIG.API_GATEWAY}${API.REFRESH_TOKEN}`,
          {},
          {
            withCredentials: true,
            headers: { "Content-Type": "application/json" },
          },
        );
      }

      const response = await refreshPromise;
      const accessToken = response.data?.data?.accessToken;

      if (!accessToken) {
        throw new Error("Missing refreshed access token");
      }

      setToken(accessToken);
      originalRequest.headers.Authorization = `Bearer ${accessToken}`;

      return httpClient(originalRequest);
    } catch (refreshError) {
      removeToken();
      return Promise.reject(refreshError);
    } finally {
      refreshPromise = null;
    }
  },
);

export default httpClient;
