import { useState } from "react";
import { login, saveLoginSession } from "../services/authService";
import "../App.css";

export default function LoginPage() {
  const [credential, setCredential] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    try {
      saveLoginSession(await login({ credential, password }));
      window.location.assign("/");
    } catch (error) {
      setMessage(error.message);
    }
  };

  return <main className="auth-page"><form className="auth-card" onSubmit={submit}>
    <h2>Iniciar sesión</h2>
    <label>Correo<input type="email" required autoComplete="email" value={credential} onChange={(event) => setCredential(event.target.value)} /></label>
    <label>Contraseña<input type="password" required autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
    <button className="primary wide">Ingresar</button>
    {message && <p>{message}</p>}
  </form></main>;
}
