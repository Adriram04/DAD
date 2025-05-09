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

import "../static/css/basurero/basureroNavBar.css";

export default function BasureroNavbar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.clear();
    navigate("/");
  };

  return (
    <nav className="basurero-navbar">
      <NavLink to="/basurero" className="basurero-logo">♻️ Basurero</NavLink>
      <ul className="basurero-nav-links">
        <li>
          <NavLink to="/basurero/contenedores">
            <FaRecycle /> <span>Contenedores</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/basurero/zonas">
            <FaMapMarkerAlt /> <span>Zonas de recogida</span>
          </NavLink>
        </li>
      </ul>

      <button className="basurero-logout-btn" onClick={handleLogout}>
        <FaSignOutAlt /> <span>Cerrar sesión</span>
      </button>
    </nav>
  );
}
