import { Link, useLocation } from "react-router-dom";
import { clearLoginSession } from "../services/authService";
import "../App.css";

export default function Navbar() {
  const location = useLocation();
  const activeClass = (path) => location.pathname === path ? "nav-btn active" : "nav-btn";
  const logout = () => { clearLoginSession(); window.location.assign("/"); };

  return <header className="inventory-header">
    <div><h1>SmartLogix Platform</h1><p>Sistema integrado de operaciones</p></div>
    <nav>
      <Link className={activeClass("/inventory")} to="/inventory">Inventario</Link>
      <Link className={activeClass("/orders")} to="/orders">Órdenes</Link>
      <Link className={activeClass("/shipments")} to="/shipments">Envíos</Link>
    </nav>
    <button className="logout-btn" onClick={logout}>Cerrar sesión</button>
  </header>;
}
