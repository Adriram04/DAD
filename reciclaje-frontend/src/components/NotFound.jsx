// src/common/NotFound.jsx
import { Link } from "react-router-dom";
import "../static/css/components/notFound.css";

export default function NotFound() {
  return (
    <div className="notfound">
      <h1>404</h1>
      <p>Vaya… la página que buscas no existe.</p>
      <Link to="/home" className="btn">Volver al inicio</Link>
    </div>
  );
}
