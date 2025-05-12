import { useEffect, useState } from "react";
import "../static/css/admin/gestionProveedores.css";
import AnimatedPage from"../components/AnimatedPage"

export default function GestionConsumidores() {
  const [consumidores, setConsumidores] = useState([]);
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
    rol: "CONSUMIDOR",
  });
  const [error, setError] = useState("");
  const token = localStorage.getItem("token");

  const fetchConsumidores = () => {
    fetch("https://api.ecobins.tech/usuarios", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => {
        const filtrados = data.usuarios.filter((u) => u.rol === "CONSUMIDOR");
        setConsumidores(filtrados);
        setFiltered(filtrados);
      })
      .catch((err) => console.error("❌ Error al obtener consumidores:", err));
  };

  useEffect(() => {
    fetchConsumidores();
  }, []);

  useEffect(() => {
    const resultados = consumidores.filter((c) =>
      c.email.toLowerCase().includes(search.toLowerCase())
    );
    setFiltered(resultados);
  }, [search, consumidores]);

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
      fetchConsumidores();
      setForm({ nombre: "", usuario: "", email: "", password: "", rol: "CONSUMIDOR" });
      setShowModal(false);
      setEditMode(false);
      setEditingId(null);
    } else {
      setError("❌ Error al guardar consumidor");
    }
  };

  const confirmDelete = async () => {
    await fetch(`https://api.ecobins.tech/usuarios/${confirmDeleteId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    setConfirmDeleteId(null);
    fetchConsumidores();
  };

  const handleEdit = (cons) => {
    setForm({
      nombre: cons.nombre,
      usuario: cons.usuario,
      email: cons.email,
      password: "",
      rol: cons.rol,
    });
    setEditingId(cons.id);
    setEditMode(true);
    setShowModal(true);
  };

  return (
	<AnimatedPage>
	<div className="gestion-proveedores">
	      <div className="header">
	        <h2>🧍 Consumidores</h2>
	        <button className="add-btn" onClick={() => {
	          setEditMode(false);
	          setForm({ nombre: "", usuario: "", email: "", password: "", rol: "CONSUMIDOR" });
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
	          <p>No hay consumidores registrados.</p>
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
	            <h3>{editMode ? "Editar Consumidor" : "Nuevo Consumidor"}</h3>
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
	                  setForm({ nombre: "", usuario: "", email: "", password: "", rol: "CONSUMIDOR" });
	                }}>Cancelar</button>
	              </div>
	            </form>
	          </div>
	        </div>
	      )}

	      {confirmDeleteId && (
	        <div className="modal-overlay">
	          <div className="modal">
	            <h3>¿Eliminar consumidor?</h3>
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