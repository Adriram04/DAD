import { Outlet } from "react-router-dom";
import ConsumidorNavbar from "./ConsumidorNavbar";
import "../static/css/consumidor/consumidorLayout.css";

export default function ConsumidorDashboard() {
  return (
    <div>
      <ConsumidorNavbar />
      <main className="consumidor-main-content">
        <Outlet />
      </main>
    </div>
  );
}