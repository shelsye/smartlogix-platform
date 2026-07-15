const API_URL = "/api";

export async function fetchWithAuth(endpoint, options = {}) {
  const token = localStorage.getItem("smartlogix_token");
  const response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  });

  if (!response.ok) {
    let message = `Error ${response.status}`;
    try {
      const body = await response.json();
      message = body.message || body.error || message;
    } catch {
      // La respuesta no contiene JSON.
    }
    throw new Error(message);
  }

  return response.status === 204 ? null : response.json();
}

export { API_URL };
