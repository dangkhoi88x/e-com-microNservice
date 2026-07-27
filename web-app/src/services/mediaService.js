import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const uploadMediaImage = async (file, purpose) => {
  const formData = new FormData();
  formData.append("file", file);
  const response = await httpClient.post(`${API.MEDIA}/images`, formData, {
    params: { purpose },
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data;
};

export const uploadProductImage = (file) => uploadMediaImage(file, "PRODUCT_IMAGE");
export const uploadAvatar = (file) => uploadMediaImage(file, "AVATAR");
export const uploadReviewImage = (file) => uploadMediaImage(file, "REVIEW_IMAGE");

export const deleteMedia = async (mediaId) => {
  await httpClient.delete(`${API.MEDIA}/${mediaId}`);
};
