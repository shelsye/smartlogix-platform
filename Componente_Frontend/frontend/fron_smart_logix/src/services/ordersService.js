import { fetchWithAuth } from "../api/apiClient";

export const getOrders = () => fetchWithAuth("/orders");
export const getMyOrders = () => fetchWithAuth("/orders/my");
export const createOrder = (payload) => fetchWithAuth("/orders", {
  method: "POST",
  body: JSON.stringify(payload),
});
export const updateMyOrder = (orderNumber, payload) => fetchWithAuth(`/orders/my/${orderNumber}`, {
  method: "PUT",
  body: JSON.stringify(payload),
});
export const cancelMyOrder = (orderNumber) => fetchWithAuth(`/orders/my/${orderNumber}`, { method: "DELETE" });
export const updateOrderStatus = (orderNumber, payload) => fetchWithAuth(`/orders/${orderNumber}/status`, {
  method: "PATCH",
  body: JSON.stringify(payload),
});
export const deleteOrder = (orderNumber) => fetchWithAuth(`/orders/${orderNumber}`, { method: "DELETE" });
export const getMyReceipt = (orderNumber) => fetchWithAuth(`/orders/my/${orderNumber}/receipt`);
export const getReceipt = (orderNumber) => fetchWithAuth(`/orders/${orderNumber}/receipt`);
