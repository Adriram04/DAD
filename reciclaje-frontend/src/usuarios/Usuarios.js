import { useEffect, useState } from "react";
import "../static/css/usuario/Usuarios.css"; // lo crearemos ahora

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState([]);

  useEffect(() => {
    fetch("https://ecobins.tech/usuarios")
      .then(res => res.json())
      .then(data => setUsuarios(data))
      .catch(err => console.error("Error al obtener usuarios:", err));
  }, []);
  
  console.log(usuarios);  // Verifica que los datos se reciban correctamente

  return (
    <div className="usuarios-container">
      <h1>Usuarios del sistema ♻️</h1>
      <div className="usuarios-grid">
        {usuarios.map((u, i) => (
          <div key={i} className="usuario-card">
            <h2>{u.map.nombre}</h2>  {/* Accedemos a u.map.nombre */}
            <p><strong>Email:</strong> {u.map.email}</p> {/* Accedemos a u.map.email */}
            <p><strong>Rol:</strong> {u.map.rol}</p>  {/* Accedemos a u.map.rol */}
          </div>
        ))}
      </div>
    </div>
  );
}
