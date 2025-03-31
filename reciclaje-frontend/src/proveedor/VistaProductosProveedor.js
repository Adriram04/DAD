// src/proveedor/VistaProductosProveedor.jsx
import { useEffect, useState } from "react";
import "../static/css/proveedor/vistaProveedor.css";

export default function VistaProductosProveedor() {
  const [productos, setProductos] = useState([]);
  const [nombre, setNombre] = useState("");
  const [puntosNecesarios, setPuntosNecesarios] = useState("");
  const [mensaje, setMensaje] = useState("");
  
  const idProveedor = localStorage.getItem("idUsuario"); // Ajustar según tu lógica

  useEffect(() => {
    fetchProductos();
  }, []);

  const fetchProductos = async () => {
    try {
      setMensaje("");
      const res = await fetch("/api/productos");
      const data = await res.json();
      if (data.productos) {
        const prodsProveedor = data.productos.filter(
          (p) => p.id_proveedor === Number(idProveedor)
        );
        setProductos(prodsProveedor);
      }
    } catch (err) {
      console.error("Error al obtener productos:", err);
      setMensaje("Error al obtener productos");
    }
  };

  const handleCrearProducto = async (e) => {
    e.preventDefault();
    if (!nombre || !puntosNecesarios) {
      setMensaje("Completa todos los campos");
      return;
    }

    try {
      const res = await fetch("/api/productos", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          nombre,
          puntos_necesarios: parseInt(puntosNecesarios),
          id_proveedor: parseInt(idProveedor),
        }),
      });
      if (!res.ok) {
        setMensaje("Error al crear producto");
        return;
      }
      setMensaje("Producto creado con éxito");
      fetchProductos();
      setNombre("");
      setPuntosNecesarios("");
    } catch (err) {
      console.error(err);
      setMensaje("Error en la petición");
    }
  };

  return (
    <div className="vista-productos">
      <h2>Mis Productos</h2>
      {mensaje && <p className="mensaje-estado">{mensaje}</p>}

      <table className="productos-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nombre</th>
            <th>Puntos Necesarios</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {productos.map((prod) => (
            <tr key={prod.id}>
              <td>{prod.id}</td>
              <td>{prod.nombre}</td>
              <td>{prod.puntos_necesarios}</td>
              <td>
                {/* Botones de ejemplo */}
                <button className="btn-accion">Editar</button>
                <button className="btn-accion">Borrar</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <hr />

      <h3>Crear nuevo producto</h3>
      <form onSubmit={handleCrearProducto} className="nuevo-producto-form">
        <div className="form-row">
          <label>Nombre:</label>
          <input
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
          />
        </div>
        <div className="form-row">
          <label>Puntos necesarios:</label>
          <input
            type="number"
            value={puntosNecesarios}
            onChange={(e) => setPuntosNecesarios(e.target.value)}
            required
          />
        </div>
        <button type="submit" className="btn-submit">Crear</button>
      </form>
    </div>
  );
}
