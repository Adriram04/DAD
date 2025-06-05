import { useEffect, useState } from "react";
import "../static/css/admin/gestionProveedores.css";
import AnimatedPage from "../components/AnimatedPage";

const API = "https://api.ecobins.tech";

export default function GestionProveedores() {
  const token = localStorage.getItem("token");

  const [proveedores,      setProveedores]      = useState([]);
  const [filtered,         setFiltered]         = useState([]);
  const [search,           setSearch]           = useState("");
  const [isLoading,        setLoading]          = useState(true);
  const [error,            setError]            = useState("");

  /* modal */
  const [showModal,        setShowModal]        = useState(false);
  const [editMode,         setEditMode]         = useState(false);
  const [editingId,        setEditingId]        = useState(null);
  const [confirmDeleteId,  setConfirmDeleteId]  = useState(null);

  const [form, setForm] = useState({
    nombre:"", usuario:"", email:"", password:"", rol:"PROVEEDOR",
  });

  /* -------- fetch -------- */
  const fetchProveedores = async () => {
    setLoading(true); setError("");
    try {
      const res = await fetch(`${API}/usuarios`, {
        headers: { Authorization:`Bearer ${token}` },
      });
      if (!res.ok) throw new Error(await res.text());
      const data = await res.json();
      const lista = data.usuarios.filter(u => u.rol === "PROVEEDOR");
      setProveedores(lista);
      setFiltered(lista.filter(p =>
        p.email.toLowerCase().includes(search.toLowerCase())
      ));
    } catch (err) {
      setError(err.message || "Error al obtener proveedores");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchProveedores(); /* eslint-disable-next-line */ }, []);

  useEffect(() => {
    const res = proveedores.filter(p =>
      p.email.toLowerCase().includes(search.toLowerCase())
    );
    setFiltered(res);
  }, [search, proveedores]);

  /* -------- helpers -------- */
  const handleChange = e =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async e => {
    e.preventDefault(); setError("");
    const endpoint = editMode
      ? `${API}/usuarios/${editingId}`
      : `${API}/usuarios`;
    const method   = editMode ? "PUT" : "POST";

    const body = { ...form };
    if (editMode) delete body.password;

    const res = await fetch(endpoint, {
      method,
      headers:{
        "Content-Type":"application/json",
        Authorization:`Bearer ${token}`,
      },
      body: JSON.stringify(body),
    });

    if (res.ok) {
      fetchProveedores();
      setForm({ nombre:"",usuario:"",email:"",password:"",rol:"PROVEEDOR" });
      setShowModal(false); setEditMode(false); setEditingId(null);
    } else {
      setError("❌ Error al guardar proveedor");
    }
  };

  const confirmDelete = async () => {
    await fetch(`${API}/usuarios/${confirmDeleteId}`,{
      method:"DELETE",
      headers:{ Authorization:`Bearer ${token}` },
    });
    setConfirmDeleteId(null);
    fetchProveedores();
  };

  const handleEdit = p => {
    setForm({ nombre:p.nombre, usuario:p.usuario, email:p.email, password:"", rol:p.rol });
    setEditingId(p.id); setEditMode(true); setShowModal(true);
  };

  /* -------- UI -------- */
  return (
    <AnimatedPage>
      <section className="gestion-contenedor">
        {/* header */}
        <div className="header">
          <h2>🏪 Proveedores</h2>
          <div className="header-actions">
            <button className="refresh-btn" onClick={fetchProveedores}>⟳ Refrescar</button>
            <button className="add-btn" onClick={()=>{
              setEditMode(false);
              setForm({ nombre:"",usuario:"",email:"",password:"",rol:"PROVEEDOR" });
              setShowModal(true);
            }}>+ Añadir</button>
          </div>
        </div>

        {/* buscador */}
        <div className="search-bar-container">
          <input
            type="text"
            className="search-bar"
            placeholder="🔍 Buscar por email…"
            value={search}
            onChange={e=>setSearch(e.target.value)}
          />
        </div>

        {/* grid */}
        {isLoading ? (
          <div className="card-grid">
            {Array.from({length:6}).map((_,i)=><div key={i} className="skeleton"/>)}
          </div>
        ) : error ? (
          <p className="error">{error}</p>
        ) : (
          <div className="card-grid">
            {filtered.length === 0 ? (
              <p>No hay proveedores registrados.</p>
            ) : (
              filtered.map(u => (
                <div key={u.id} className="card">
                  <strong>{u.nombre}</strong>
                  <p><b>Usuario:</b> {u.usuario}</p>
                  <p><b>Email:</b> {u.email}</p>

                  <div className="card-actions">
                    <button title="Eliminar" onClick={()=>setConfirmDeleteId(u.id)}>🗑️</button>
                    <button title="Editar"   onClick={()=>handleEdit(u)}>✏️</button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* modal crear / editar */}
        {showModal && (
          <div className="modal-overlay">
            <div className="modal">
              <h3>{editMode ? "Editar Proveedor" : "Nuevo Proveedor"}</h3>
              <form onSubmit={handleSubmit}>
                <input name="nombre"  placeholder="Nombre"  value={form.nombre}  onChange={handleChange} required />
                <input name="usuario" placeholder="Usuario" value={form.usuario} onChange={handleChange} required />
                <input name="email"   type="email" placeholder="Email"
                       value={form.email} onChange={handleChange} required />
                {!editMode && (
                  <input name="password" type="password" placeholder="Contraseña"
                         value={form.password} onChange={handleChange} required />
                )}
                {error && <p className="error">{error}</p>}
                <div className="modal-buttons">
                  <button type="submit">{editMode ? "Guardar" : "Crear"}</button>
                  <button type="button" onClick={()=>{ setShowModal(false); setEditMode(false);
                    setForm({ nombre:"",usuario:"",email:"",password:"",rol:"PROVEEDOR" });
                  }}>Cancelar</button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* confirmar borrado */}
        {confirmDeleteId && (
          <div className="modal-overlay">
            <div className="modal">
              <h3>¿Eliminar proveedor?</h3>
              <p>Esta acción no se puede deshacer.</p>
              <div className="modal-buttons">
                <button onClick={confirmDelete}>Eliminar</button>
                <button onClick={()=>setConfirmDeleteId(null)}>Cancelar</button>
              </div>
            </div>
          </div>
        )}
      </section>
    </AnimatedPage>
  );
}
