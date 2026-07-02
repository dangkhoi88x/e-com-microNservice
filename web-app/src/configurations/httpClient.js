import axios from "axios";
import { CONFIG } from "./configuration";
import { getToken } from "../services/localStorageService.js";

const httpClient = axios.create({
  baseURL: CONFIG.API_GATEWAY,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

httpClient.interceptors.request.use((config) => {
  const token = getToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;  //mọi API cần token sẽ tự có Bearer token.
  }

  return config;
});

export default httpClient;