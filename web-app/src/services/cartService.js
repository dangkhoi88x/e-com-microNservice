import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

const emptyCart = () => ({ items: [], totalItems: 0, totalAmount: 0 });

const notifyCartChanged = (cart) => {
  window.dispatchEvent(new CustomEvent("nova:cart-changed", { detail: cart }));
  return cart;
};

const data = (response) => response.data?.data || emptyCart();

export const getMyCart = async () => data(await httpClient.get(API.CART));

export const addCartItem = async ({ productId, variantId = null, quantity = 1 }) =>
  notifyCartChanged(data(await httpClient.post(`${API.CART}/items`, { productId, variantId, quantity })));

export const updateCartItem = async (itemId, { quantity, selected }) =>
  notifyCartChanged(data(await httpClient.put(`${API.CART}/items/${itemId}`, { quantity, selected })));

export const removeCartItem = async (itemId) => {
  await httpClient.delete(`${API.CART}/items/${itemId}`);
  return notifyCartChanged(await getMyCart());
};

export const clearCart = async () => {
  await httpClient.delete(`${API.CART}/items`);
  return notifyCartChanged(emptyCart());
};

export const cartQuantity = (cart) => (cart?.items || []).reduce(
  (total, item) => total + Number(item.quantity || 0),
  0,
);
