import { fetchWithAuth } from "./apiClient";

async function request(method, url, data, config = {}) {
  const responseData = await fetchWithAuth(url, {
    method,
    body: data === undefined ? undefined : JSON.stringify(data),
    headers: config.headers,
  });
  return { data: responseData, status: responseData === null ? 204 : 200 };
}

// Adaptador conservado para compatibilidad con imports heredados, sin añadir
// una segunda librería HTTP ni una segunda configuración de autenticación.
const axiosClient = {
  get: (url, config) => request("GET", url, undefined, config),
  post: (url, data, config) => request("POST", url, data, config),
  put: (url, data, config) => request("PUT", url, data, config),
  patch: (url, data, config) => request("PATCH", url, data, config),
  delete: (url, config) => request("DELETE", url, undefined, config),
};

export default axiosClient;
