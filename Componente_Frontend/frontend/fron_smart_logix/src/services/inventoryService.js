import { fetchWithAuth } from "../api/apiClient";

export const getInventory = () => fetchWithAuth("/inventory/items");
export const getCatalog = () => fetchWithAuth("/inventory/catalog");
export const getInventoryStatistics = () => fetchWithAuth("/inventory/statistics");
export const createInventoryItem = (payload) => fetchWithAuth("/inventory/items", {
  method: "POST",
  body: JSON.stringify(payload),
});
export const updateInventoryItem = (sku, payload) => fetchWithAuth(`/inventory/items/${sku}`, {
  method: "PUT",
  body: JSON.stringify(payload),
});
export const updateInventoryStock = (sku, availableQuantity) => fetchWithAuth(`/inventory/items/${sku}/stock`, {
  method: "PATCH",
  body: JSON.stringify({ availableQuantity }),
});
export const deleteInventoryItem = (sku) => fetchWithAuth(`/inventory/items/${sku}`, { method: "DELETE" });
