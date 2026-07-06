import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const getCategories = async () => {
  const response = await httpClient.get(API.CATEGORIES);
  return response.data?.data || [];
};

export const createCategory = async (payload) => {
  const response = await httpClient.post(API.CATEGORIES, payload);
  return response.data?.data;
};

export const updateCategory = async (id, payload) => {
  const response = await httpClient.put(`${API.CATEGORIES}/${id}`, payload);
  return response.data?.data;
};

export const deleteCategory = async (id) => {
  await httpClient.delete(`${API.CATEGORIES}/${id}`);
};
