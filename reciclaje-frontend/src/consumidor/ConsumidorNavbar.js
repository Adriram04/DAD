// src/consumidor/ConsumidorNavbar.js
import { useNavigate, NavLink } from "react-router-dom";
import {
  FaRecycle,
  FaSignOutAlt,
  FaTrophy,
  FaHome 
} from "react-icons/fa";

import "../static/css/consumidor/consumidorNavbar.css";

export default function ConsumidorNavbar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.clear();
    navigate("/");
  };

  return (
    <nav className="consumidor-navbar">
      <NavLink to="/consumidor" className="consumidor-logo">♻️ ConsumidorPanel</NavLink>
      <ul className="consumidor-nav-links">
	  	<li>
	        <NavLink to="/consumidor/home">
	          <FaHome  /> <span>Home</span>
	        </NavLink>
        </li>
        <li>
          <NavLink to="/consumidor/ecoLiga">
            <FaTrophy /> <span>EcoLiga</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/consumidor/contenedores">
            <FaRecycle /> <span>Contenedores</span>
          </NavLink>
        </li>
      </ul>

      <button className="consumidor-logout-btn" onClick={handleLogout}>
        <FaSignOutAlt /> <span>Cerrar sesión</span>
      </button>
    </nav>
  );
}
