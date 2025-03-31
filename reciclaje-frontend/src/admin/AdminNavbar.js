// src/admin/AdminNavbar.js
import { useNavigate, NavLink } from "react-router-dom";
import {
  FaUsers,
  FaStore,
  FaUserAlt,
  FaTrash,
  FaRecycle,
  FaSignOutAlt,
  FaMapMarkerAlt
} from "react-icons/fa";

import "../static/css/admin/adminNavbar.css";

export default function AdminNavbar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.clear();
    navigate("/");
  };

  return (
    <nav className="admin-navbar">
	  <NavLink to="/admin" className="admin-logo">♻️ AdminPanel</NavLink>
      <ul className="admin-nav-links">
        <li>
          <NavLink to="/admin/usuarios">
            <FaUsers /> <span>Usuarios</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/admin/proveedores">
            <FaStore /> <span>Proveedores</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/admin/consumidores">
            <FaUserAlt /> <span>Consumidores</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/admin/basureros">
            <FaTrash /> <span>Basureros</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/admin/contenedores">
            <FaRecycle /> <span>Contenedores</span>
          </NavLink>
        </li>
		<li>
          <NavLink to="/admin/zonas">
            <FaMapMarkerAlt /> <span>Zonas de recogida</span>
          </NavLink>
        </li>
      </ul>

      <button className="admin-logout-btn" onClick={handleLogout}>
        <FaSignOutAlt /> <span>Cerrar sesión</span>
      </button>
    </nav>
  );
}
