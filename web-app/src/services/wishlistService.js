import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { isAuthenticated } from "./authenticationService";

const KEY = "nova-shop-wishlist";
const readLocal = () => { try { const saved = JSON.parse(localStorage.getItem(KEY) || "[]"); return Array.isArray(saved) ? saved : []; } catch { return []; } };
const saveLocal = (items) => localStorage.setItem(KEY, JSON.stringify(items));
const productId = (item) => item.productId || item.id;
const toRequest = (item) => ({ productId: productId(item), variantId: item.variantId || null, productName: item.name || item.productName, price: item.price, imageUrl: item.imageUrl || item.images?.find((image) => image.isPrimary)?.url || item.images?.[0]?.url || null, categoryName: item.categoryName || null });
const toItem = (item) => ({ ...item, id: item.productId, name: item.productName || item.name });
const emitWishlistChanged = (items, action) => window.dispatchEvent(new CustomEvent("nova:wishlist-changed", { detail: { items, action } }));

export const getWishlist = readLocal;
export const loadWishlist = async () => {
  if (!isAuthenticated()) return readLocal();
  const guestItems = readLocal();
  if (guestItems.length) await Promise.all(guestItems.map((item) => httpClient.post(`${API.WISHLIST}/items`, toRequest(item)).catch(() => null)));
  const response = await httpClient.get(API.WISHLIST);
  const items = (response.data?.data || []).map(toItem);
  saveLocal(items);
  return items;
};
export const toggleWishlist = (product) => {
  const current = readLocal(); const id = productId(product);
  const exists = current.some((item) => productId(item) === id && (item.variantId || null) === (product.variantId || null));
  const next = exists ? current.filter((item) => !(productId(item) === id && (item.variantId || null) === (product.variantId || null))) : [...current, product];
  saveLocal(next);
  emitWishlistChanged(next, exists ? "removed" : "added");
  if (isAuthenticated()) { const request = toRequest(product); const call = exists ? httpClient.delete(`${API.WISHLIST}/items/${request.productId}`, { params: { variantId: request.variantId || undefined } }) : httpClient.post(`${API.WISHLIST}/items`, request); call.catch(() => {}); }
  return next;
};
export const toggleWishlistTransaction = async (product) => {
  const previous = readLocal(); const id = productId(product);
  const exists = previous.some((item) => productId(item) === id && (item.variantId || null) === (product.variantId || null));
  const next = exists ? previous.filter((item) => !(productId(item) === id && (item.variantId || null) === (product.variantId || null))) : [...previous, product];
  saveLocal(next);
  emitWishlistChanged(next, exists ? "removed" : "added");
  if (!isAuthenticated()) return { items: next, action: exists ? "removed" : "added", synced: false };
  try {
    const request = toRequest(product);
    if (exists) await httpClient.delete(`${API.WISHLIST}/items/${request.productId}`, { params: { variantId: request.variantId || undefined } });
    else await httpClient.post(`${API.WISHLIST}/items`, request);
    return { items: next, action: exists ? "removed" : "added", synced: true };
  } catch (error) {
    saveLocal(previous);
    emitWishlistChanged(previous, "reverted");
    throw error;
  }
};
export const removeWishlistItem = (item) => {
  const current = readLocal(); const id = productId(item);
  const next = current.filter((entry) => !(productId(entry) === id && (entry.variantId || null) === (item.variantId || null)));
  saveLocal(next);
  emitWishlistChanged(next, "removed");
  if (isAuthenticated()) httpClient.delete(`${API.WISHLIST}/items/${id}`, { params: { variantId: item.variantId || undefined } }).catch(() => {});
  return next;
};
