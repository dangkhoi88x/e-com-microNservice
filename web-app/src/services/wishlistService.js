import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { isAuthenticated } from "./authenticationService";

const productId = (item) => item.productId || item.id;
const toRequest = (item) => ({
  productId: productId(item),
  variantId: item.variantId || null,
  productName: item.name || item.productName,
  price: item.price,
  imageUrl: item.imageUrl || item.images?.find((image) => image.isPrimary)?.url || item.images?.[0]?.url || null,
  categoryName: item.categoryName || null,
});
const toItem = (item) => ({ ...item, name: item.productName || item.name });
const sameWishlistItem = (item, request) => productId(item) === request.productId
  && (item.variantId || null) === (request.variantId || null);
const emitWishlistChanged = (items, action) => window.dispatchEvent(
  new CustomEvent("nova:wishlist-changed", { detail: { items, action } }),
);

export const loadWishlist = async () => {
  if (!isAuthenticated()) return [];

  const response = await httpClient.get(API.WISHLIST);
  return (response.data?.data || []).map(toItem);
};

export const toggleWishlist = async (product, currentItems) => {
  if (!isAuthenticated()) throw new Error("AUTHENTICATION_REQUIRED");

  const request = toRequest(product);
  const exists = currentItems.some((item) => sameWishlistItem(item, request));

  if (exists) {
    await httpClient.delete(`${API.WISHLIST}/items/${request.productId}`, {
      params: { variantId: request.variantId || undefined },
    });
  } else {
    await httpClient.post(`${API.WISHLIST}/items`, request);
  }

  const items = await loadWishlist();
  emitWishlistChanged(items, exists ? "removed" : "added");
  return { items, action: exists ? "removed" : "added" };
};

export const removeWishlistItem = async (item) => {
  if (!isAuthenticated()) throw new Error("AUTHENTICATION_REQUIRED");

  const request = toRequest(item);
  await httpClient.delete(`${API.WISHLIST}/items/${request.productId}`, {
    params: { variantId: request.variantId || undefined },
  });

  const items = await loadWishlist();
  emitWishlistChanged(items, "removed");
  return items;
};
