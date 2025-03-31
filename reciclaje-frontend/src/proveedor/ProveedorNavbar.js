// src/proveedor/ProveedorNavbar.jsx
import { useNavigate, NavLink } from "react-router-dom";
import {
  FaBoxOpen,
  FaUserAlt,
  FaDollarSign,
  FaPercent,
  FaSignOutAlt
} from "react-icons/fa";

import "../static/css/proveedor/proveedorNavbar.css";

export default function ProveedorNavbar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.clear();
    navigate("/");
  };

  return (
    <nav className="proveedor-navbar">
      <NavLink to="/proveedor" className="proveedor-navbar__logo">
        🛒 ProveedorPanel
      </NavLink>

      <ul className="proveedor-navbar__links">
        <li>
          <NavLink to="/proveedor">
            <FaBoxOpen /> <span>Productos</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/proveedor/mis-ventas">
            <FaDollarSign /> <span>Mis Ventas</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/proveedor/perfil">
            <FaUserAlt /> <span>Mi Perfil</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/proveedor/promociones">
            <FaPercent /> <span>Promociones</span>
          </NavLink>
        </li>
      </ul>

      <button className="proveedor-navbar__logout" onClick={handleLogout}>
        <FaSignOutAlt /> <span>Cerrar sesión</span>
      </button>
    </nav>
  );
}
