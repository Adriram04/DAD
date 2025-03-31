// src/proveedor/ProveedorDashboard.jsx
import { Outlet } from "react-router-dom";
import ProveedorNavbar from "./ProveedorNavbar";
import "../static/css/proveedor/proveedorLayout.css";

export default function ProveedorDashboard() {
  return (
    <div className="proveedor-dashboard">
      <ProveedorNavbar />
      <main className="proveedor-main-content">
        <Outlet />
      </main>
    </div>
  );
}
