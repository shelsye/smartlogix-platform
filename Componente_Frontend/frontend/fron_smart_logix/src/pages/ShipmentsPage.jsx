import { useEffect, useState } from "react";
import Navbar from "../components/Navbar";
import {
  acceptRouteSelection,
  getCurrentRouteSelection,
  getRegions,
  getRouteRecommendations,
} from "../services/shipmentsService";
import "../App.css";

export default function ShipmentsPage() {
  const [regions, setRegions] = useState([]);
  const [region, setRegion] = useState("");
  const [units, setUnits] = useState(1);
  const [routes, setRoutes] = useState([]);
  const [acceptedRoute, setAcceptedRoute] = useState(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    Promise.all([getRegions(), getCurrentRouteSelection()])
      .then(([regionData, selection]) => {
        setRegions(regionData);
        setRegion(selection?.region || regionData[0] || "");
        if (selection) {
          setAcceptedRoute(selection);
          setUnits(selection.totalUnits);
        }
      })
      .catch((error) => setMessage(error.message));
  }, []);

  const calculate = async () => {
    try {
      setRoutes(await getRouteRecommendations(region, Number(units)));
      setMessage("");
    } catch (error) {
      setMessage(error.message);
    }
  };

  const accept = async (route) => {
    try {
      const saved = await acceptRouteSelection(region, Number(units), route.type);
      setAcceptedRoute(saved);
      setMessage("Ruta aceptada y guardada correctamente.");
    } catch (error) {
      setMessage(error.message);
    }
  };

  return <div className="inventory-container">
    <Navbar />
    <section className="panel">
      <div className="panel-head"><div><h2>Adaptador inteligente de envíos</h2><p>Las tres alternativas se calculan en shipment-service.</p></div></div>
      <div className="form-grid">
        <label>Región<select value={region} onChange={(event) => setRegion(event.target.value)}>{regions.map((name) => <option key={name}>{name}</option>)}</select></label>
        <label>Unidades<input type="number" min="1" value={units} onChange={(event) => setUnits(event.target.value)} /></label>
      </div>
      <button className="primary" disabled={!region} onClick={calculate}>Calcular alternativas</button>
      {message && <p>{message}</p>}
      <div className="route-list">{routes.map((route) => <article className="route-option" key={route.type}>
        <div><b>{route.routeName}</b><span>{route.carrier}</span><small>{route.distanceKm} km · {route.estimatedDays} días · {route.estimatedDate}</small></div>
        <strong>{Number(route.price || 0).toLocaleString("es-CL", { style: "currency", currency: "CLP" })}</strong>
        <button className="primary accept-route" type="button" onClick={() => accept(route)}>
          {acceptedRoute?.type === route.type && acceptedRoute?.region === region && acceptedRoute?.totalUnits === Number(units)
            ? "Ruta aceptada" : "Aceptar ruta"}
        </button>
      </article>)}</div>
    </section>
  </div>;
}
