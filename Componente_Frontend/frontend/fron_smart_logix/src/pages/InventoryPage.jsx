import { useEffect, useMemo, useState } from "react";
import Navbar from "../components/Navbar";
import { getInventory } from "../services/inventoryService";
import "../App.css";

export default function InventoryPage() {
  const [items, setItems] = useState([]);
  const [search, setSearch] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    getInventory().then(setItems).catch((error) => setMessage(error.message));
  }, []);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return items;
    return items.filter((item) => `${item.sku} ${item.productName} ${item.category}`.toLowerCase().includes(term));
  }, [items, search]);

  return <div className="inventory-container">
    <Navbar />
    <section className="panel">
      <div className="panel-head"><div><h2>Inventario administrativo</h2><p>Datos reales de inventory-service.</p></div><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar producto" /></div>
      {message && <p>{message}</p>}
      <div className="table-wrap"><table><thead><tr><th>SKU</th><th>Producto</th><th>Categoría</th><th>Precio</th><th>Disponible</th><th>Reservado</th><th>Estado</th></tr></thead><tbody>
        {filtered.map((item) => <tr key={item.sku}><td>{item.sku}</td><td>{item.productName}</td><td>{item.category}</td><td>{Number(item.price || 0).toLocaleString("es-CL", { style: "currency", currency: "CLP" })}</td><td>{item.availableQuantity}</td><td>{item.reservedQuantity}</td><td>{item.active ? "Activo" : "Inactivo"}</td></tr>)}
      </tbody></table></div>
    </section>
  </div>;
}
