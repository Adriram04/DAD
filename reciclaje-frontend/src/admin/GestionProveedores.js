import { useEffect, useState } from "react";
import "../static/css/admin/gestionProveedores.css";
import AnimatedPage from"../components/AnimatedPage"

export default function GestionProveedores() {
  const [proveedores, setProveedores] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [search, setSearch] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);
  const [form, setForm] = useState({
    nombre: "",
    usuario: "",
    email: "",
    password: "",
    rol: "PROVEEDOR",
  });
  const [error, setError] = useState("");
  const token = localStorage.getItem("token");

  const fetchProveedores = () => {
    fetch("https://api.ecobins.tech/usuarios", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => {
        const filtrados = data.usuarios.filter((u) => u.rol === "PROVEEDOR");
        setProveedores(filtrados);
        setFiltered(filtrados);
      })
      .catch((err) => console.error("❌ Error al obtener proveedores:", err));
  };

  useEffect(() => {
    fetchProveedores();
  }, []);

  useEffect(() => {
    const resultados = proveedores.filter(p =>
      p.email.toLowerCase().includes(search.toLowerCase())
    );
    setFiltered(resultados);
  }, [search, proveedores]);

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const endpoint = editMode
      ? `https://api.ecobins.tech/usuarios/${editingId}`
      : "https://api.ecobins.tech/usuarios";
    const method = editMode ? "PUT" : "POST";

    const body = { ...form };
    if (!editMode) delete body.id;
    if (editMode) delete body.password;

    const res = await fetch(endpoint, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    });

    if (res.ok) {
      fetchProveedores();
      setForm({ nombre: "", usuario: "", email: "", password: "", rol: "PROVEEDOR" });
      setShowModal(false);
      setEditMode(false);
      setEditingId(null);
    } else {
      setError("❌ Error al guardar proveedor");
    }
  };

  const confirmDelete = async () => {
    await fetch(`https://api.ecobins.tech/usuarios/${confirmDeleteId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    setConfirmDeleteId(null);
    fetchProveedores();
  };

  const handleEdit = (prov) => {
    setForm({
      nombre: prov.nombre,
      usuario: prov.usuario,
      email: prov.email,
      password: "",
      rol: prov.rol,
    });
    setEditingId(prov.id);
    setEditMode(true);
    setShowModal(true);
  };

  return (
    <AnimatedPage>
	<div className="gestion-proveedores">
	      <div className="header">
	        <h2>🏪 Proveedores</h2>
	        <button className="add-btn" onClick={() => {
	          setEditMode(false);
	          setForm({ nombre: "", usuario: "", email: "", password: "", rol: "PROVEEDOR" });
	          setShowModal(true);
	        }}>+ Añadir</button>
	      </div>

	      <div className="search-bar-container">
	        <input
	          type="text"
	          className="search-bar"
	          placeholder="🔍 Buscar por email..."
	          value={search}
	          onChange={(e) => setSearch(e.target.value)}
	        />
	      </div>

	      <div className="card-grid">
	        {filtered.length === 0 ? (
	          <p>No hay proveedores registrados.</p>
	        ) : (
	          filtered.map((u) => (
	            <div key={u.id} className="card">
	              <strong>{u.nombre}</strong>
	              <p><b>Usuario:</b> {u.usuario}</p>
	              <p><b>Email:</b> {u.email}</p>
	              <div className="card-actions">
	                <button onClick={() => setConfirmDeleteId(u.id)}>🗑️</button>
	                <button onClick={() => handleEdit(u)}>✏️</button>
	              </div>
	            </div>
	          ))
	        )}
	      </div>

	      {showModal && (
	        <div className="modal-overlay">
	          <div className="modal">
	            <h3>{editMode ? "Editar Proveedor" : "Nuevo Proveedor"}</h3>
	            <form onSubmit={handleSubmit}>
	              <input name="nombre" placeholder="Nombre" value={form.nombre} onChange={handleChange} required />
	              <input name="usuario" placeholder="Usuario" value={form.usuario} onChange={handleChange} required />
	              <input name="email" placeholder="Email" type="email" value={form.email} onChange={handleChange} required />
	              {!editMode && (
	                <input name="password" placeholder="Contraseña" type="password" value={form.password} onChange={handleChange} required />
	              )}
	              {error && <p className="error">{error}</p>}
	              <div className="modal-buttons">
	                <button type="submit">{editMode ? "Guardar" : "Crear"}</button>
	                <button type="button" onClick={() => {
	                  setShowModal(false);
	                  setEditMode(false);
	                  setForm({ nombre: "", usuario: "", email: "", password: "", rol: "PROVEEDOR" });
	                }}>Cancelar</button>
	              </div>
	            </form>
	          </div>
	        </div>
	      )}

	      {confirmDeleteId && (
	        <div className="modal-overlay">
	          <div className="modal">
	            <h3>¿Eliminar proveedor?</h3>
	            <p>Esta acción no se puede deshacer.</p>
	            <div className="modal-buttons">
	              <button onClick={confirmDelete}>Eliminar</button>
	              <button onClick={() => setConfirmDeleteId(null)}>Cancelar</button>
	            </div>
	          </div>
	        </div>
	      )}
	    </div>
	</AnimatedPage>
  );
}