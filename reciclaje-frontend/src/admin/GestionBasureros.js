import { useEffect, useState } from "react";
import "../static/css/admin/gestionProveedores.css";
import AnimatedPage from"../components/AnimatedPage"

export default function GestionBasureros() {
  const [basureros, setBasureros] = useState([]);
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
    rol: "BASURERO",
  });
  const [error, setError] = useState("");
  const token = localStorage.getItem("token");

  const fetchBasureros = () => {
    fetch("http://localhost:8080/api/usuarios", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => {
        const filtrados = data.usuarios.filter((u) => u.rol === "BASURERO");
        setBasureros(filtrados);
        setFiltered(filtrados);
      })
      .catch((err) => console.error("❌ Error al obtener basureros:", err));
  };

  useEffect(() => {
    fetchBasureros();
  }, []);

  useEffect(() => {
    const resultados = basureros.filter(p =>
      p.email.toLowerCase().includes(search.toLowerCase())
    );
    setFiltered(resultados);
  }, [search, basureros]);

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const endpoint = editMode
      ? `http://localhost:8080/api/usuarios/${editingId}`
      : "http://localhost:8080/api/usuarios";
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
      fetchBasureros();
      setForm({ nombre: "", usuario: "", email: "", password: "", rol: "BASURERO" });
      setShowModal(false);
      setEditMode(false);
      setEditingId(null);
    } else {
      setError("❌ Error al guardar basurero");
    }
  };

  const confirmDelete = async () => {
    await fetch(`http://localhost:8080/api/usuarios/${confirmDeleteId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    setConfirmDeleteId(null);
    fetchBasureros();
  };

  const handleEdit = (b) => {
    setForm({
      nombre: b.nombre,
      usuario: b.usuario,
      email: b.email,
      password: "",
      rol: b.rol,
    });
    setEditingId(b.id);
    setEditMode(true);
    setShowModal(true);
  };

  return (
    <AnimatedPage>
	<div className="gestion-proveedores">
	      <div className="header">
	        <h2>🧹 Basureros</h2>
	        <button className="add-btn" onClick={() => {
	          setEditMode(false);
	          setForm({ nombre: "", usuario: "", email: "", password: "", rol: "BASURERO" });
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
	          <p>No hay basureros registrados.</p>
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
	            <h3>{editMode ? "Editar Basurero" : "Nuevo Basurero"}</h3>
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
	                  setForm({ nombre: "", usuario: "", email: "", password: "", rol: "BASURERO" });
	                }}>Cancelar</button>
	              </div>
	            </form>
	          </div>
	        </div>
	      )}

	      {confirmDeleteId && (
	        <div className="modal-overlay">
	          <div className="modal">
	            <h3>¿Eliminar basurero?</h3>
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
