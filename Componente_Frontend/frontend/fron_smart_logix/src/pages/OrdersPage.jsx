import { useEffect, useState } from "react";
import Navbar from "../components/Navbar";
import { deleteOrder, getOrders } from "../services/ordersService";
import "../App.css";

export default function OrdersPage() {
  const [orders, setOrders] = useState([]);
  const [message, setMessage] = useState("");

  const load = async () => {
    try {
      setOrders(await getOrders());
      setMessage("");
    } catch (error) {
      setMessage(error.message);
    }
  };

  useEffect(() => {
    let active = true;
    getOrders()
      .then((data) => { if (active) setOrders(data); })
      .catch((error) => { if (active) setMessage(error.message); });
    return () => { active = false; };
  }, []);

  const remove = async (order) => {
    if (order.status !== "CANCELLED") {
      setMessage("Solo se pueden eliminar definitivamente órdenes canceladas.");
      return;
    }
    if (!window.confirm(`¿Eliminar definitivamente ${order.orderNumber}?`)) return;
    try {
      await deleteOrder(order.orderNumber);
      await load();
    } catch (error) {
      setMessage(error.message);
    }
  };

  return <div className="inventory-container">
    <Navbar />
    <section className="panel">
      <div className="panel-head"><div><h2>Órdenes registradas</h2><p>Información obtenida desde order-service.</p></div></div>
      {message && <p>{message}</p>}
      <div className="table-wrap"><table><thead><tr><th>Número</th><th>Cliente</th><th>Estado</th><th>Productos</th><th>Total</th><th>Acción</th></tr></thead><tbody>
        {orders.map((order) => <tr key={order.orderNumber}>
          <td>{order.orderNumber}</td><td>{order.customerName}</td><td>{order.status}</td>
          <td>{order.lines?.map((line) => `${line.productName} × ${line.quantity}`).join(", ")}</td>
          <td>{Number(order.totalAmount || 0).toLocaleString("es-CL", { style: "currency", currency: "CLP" })}</td>
          <td><button className="icon danger" disabled={order.status !== "CANCELLED"} onClick={() => remove(order)}>Eliminar</button></td>
        </tr>)}
      </tbody></table></div>
    </section>
  </div>;
}
