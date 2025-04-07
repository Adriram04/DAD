import { useEffect, useState } from "react";
import "../static/css/admin/gestionProveedores.css";
import AnimatedPage from"../components/AnimatedPage"

export default function GestionAdministradores() {
  const [usuarios, setUsuarios] = useState([]);
  const token = localStorage.getItem("token");

  const fetchUsuarios = () => {
    fetch("https://www.ecobins.tech/api/usuarios", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => {
        setUsuarios(data.usuarios);
      })
      .catch((err) => console.error("❌ Error al obtener usuarios:", err));
  };

  useEffect(() => {
    fetchUsuarios();
  }, []);

  return (
    <AnimatedPage>
	<div className="gestion-proveedores">
	      <div className="header">
	        <h2>👥 Usuarios del sistema</h2>
	      </div>

	      <div className="card-grid">
	        {usuarios.length === 0 ? (
	          <p>No hay usuarios registrados.</p>
	        ) : (
	          usuarios.map((u) => (
	            <div key={u.id} className="card">
	              <strong>{u.nombre}</strong>
	              <p><b>Usuario:</b> {u.usuario}</p>
	              <p><b>Email:</b> {u.email}</p>
	              <p><b>Rol:</b> {u.rol}</p>
	            </div>
	          ))
	        )}
	      </div>
	    </div>
	</AnimatedPage>
  );
}
