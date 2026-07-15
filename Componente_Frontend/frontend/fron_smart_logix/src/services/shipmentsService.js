import { fetchWithAuth } from "../api/apiClient";

export const getShipments = () => fetchWithAuth("/shipments");
export const getRegions = () => fetchWithAuth("/shipments/regions");
export const getRouteRecommendations = (region, units) => fetchWithAuth("/shipments/recommendations", {
  method: "POST",
  body: JSON.stringify({ region, units }),
});
export const getCurrentRouteSelection = () => fetchWithAuth("/shipments/selections/current");
export const acceptRouteSelection = (region, totalUnits, routeType) => fetchWithAuth("/shipments/selections", {
  method: "POST",
  body: JSON.stringify({ region, totalUnits, routeType }),
});
