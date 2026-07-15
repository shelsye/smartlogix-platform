import { fetchWithAuth } from "../api/apiClient";

export function login({ credential, password }) {
  return fetchWithAuth("/auth/login", {
    method: "POST",
    body: JSON.stringify({ credential, password }),
  });
}

export function register(payload) {
  return fetchWithAuth("/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function saveLoginSession(loginResponse) {
  localStorage.setItem("smartlogix_token", loginResponse.token);
  localStorage.setItem(
    "smartlogix_session",
    JSON.stringify({
      userId: loginResponse.userId,
      firstName: loginResponse.firstName,
      lastName: loginResponse.lastName,
      username: loginResponse.username,
      email: loginResponse.email,
      role: loginResponse.role,
    }),
  );
}

export function clearLoginSession() {
  localStorage.removeItem("smartlogix_token");
  localStorage.removeItem("smartlogix_session");
}
