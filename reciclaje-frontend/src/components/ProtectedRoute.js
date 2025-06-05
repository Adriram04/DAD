import { Navigate, useLocation } from "react-router-dom";

export default function ProtectedRoute({ roles, children }) {
  const token = localStorage.getItem("token");
  const rol   = localStorage.getItem("rol");
  const from  = useLocation();              // <-- para recordar a dónde iba

  // 1) No logueado ⇒ fuera
  if (!token) return <Navigate to="/" replace state={{ from }} />;

  // 2) Logueado pero sin rol adecuado
  if (roles && !roles.includes(rol))
    return <Navigate to="/home" replace />; // o a /403

  // 3) Correcto
  return children;
}
