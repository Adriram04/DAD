// src/consumidor/GestionBasureros.jsx

import { useEffect, useState } from "react";
import "../static/css/admin/gestionBasureros.css";
import AnimatedPage from "../components/AnimatedPage";

const API = "https://api.ecobins.tech";

export default function GestionBasureros() {
  const token = localStorage.getItem("token");

  /* ---- estados para basureros y búsqueda ---- */
  const [basureros, setBasureros] = useState([]);        // lista completa de basureros
  const [filtered, setFiltered] = useState([]);          // lista filtrada por búsqueda
  const [search, setSearch] = useState("");              // texto de búsqueda (por email)
  const [isLoading, setLoading] = useState(true);
  const [error, setError] = useState("");

  /* ---- estados para crear/editar basurero ---- */
  const [showModal, setShowModal] = useState(false);     // modal de crear/editar
  const [editMode, setEditMode] = useState(false);       // true = editar, false = crear
  const [editingId, setEditingId] = useState(null);      // id del basurero que estamos editando
  const [form, setForm] = useState({
    nombre: "",
    usuario: "",
    email: "",
    password: "",
    rol: "BASURERO",
  });

  /* ---- estados para confirmar borrado ---- */
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);

  /* ---- estados para asignar zona ---- */
  const [zonas, setZonas] = useState([]);                 // lista de zonas disponibles
  const [assignModalId, setAssignModalId] = useState(null);// id del basurero al que asignamos zona
  const [selectedZone, setSelectedZone] = useState("");    // zona seleccionada en el dropdown

  /* ---- FETCH: obtener todos los basureros ---- */
  const fetchBasureros = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await fetch(`${API}/usuarios`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error(await res.text());
      const data = await res.json();
      // Filtramos usuarios cuyo rol sea BASURERO
      const filtrados = data.usuarios.filter((u) => u.rol === "BASURERO");
      setBasureros(filtrados);

      // Aplicamos búsqueda sobre el email
      const fil = filtrados.filter((p) =>
        p.email.toLowerCase().includes(search.toLowerCase())
      );
      setFiltered(fil);
    } catch (err) {
      setError(err.message || "Error al obtener basureros");
    } finally {
      setLoading(false);
    }
  };

  /* ---- FETCH: obtener todas las zonas ---- */
  const fetchZonas = async () => {
    try {
      const res = await fetch(`${API}/zonas`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error("Error al obtener zonas");
      const data = await res.json();
      // data = { zonas: [ { id, nombre, canal_mqtt }, … ] }
      setZonas(data.zonas || []);
    } catch (err) {
      console.error("Error al cargar zonas:", err);
    }
  };

  /* ---- init: al montar, cargamos basureros y zonas ---- */
  useEffect(() => {
    fetchBasureros();
    fetchZonas();
    // eslint-disable-next-line
  }, []);

  /* ---- Re-filtrar cuando cambie el campo search o la lista de basureros ---- */
  useEffect(() => {
    const res = basureros.filter((p) =>
      p.email.toLowerCase().includes(search.toLowerCase())
    );
    setFiltered(res);
  }, [search, basureros]);

  /* ---- Helpers de formulario ---- */
  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  /* ---- Crear / Editar basurero ---- */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const endpoint = editMode
      ? `${API}/usuarios/${editingId}`
      : `${API}/usuarios`;
    const method = editMode ? "PUT" : "POST";

    const bodyData = { ...form };
    if (editMode) delete bodyData.password;

    try {
      const res = await fetch(endpoint, {
        method,
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(bodyData),
      });
      if (!res.ok) throw new Error("❌ Error al guardar basurero");

      fetchBasureros();
      setForm({
        nombre: "",
        usuario: "",
        email: "",
        password: "",
        rol: "BASURERO",
      });
      setShowModal(false);
      setEditMode(false);
      setEditingId(null);
    } catch (err) {
      setError(err.message);
    }
  };

  /* ---- Confirmar borrado ---- */
  const confirmDelete = async () => {
    try {
      await fetch(`${API}/usuarios/${confirmDeleteId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      setConfirmDeleteId(null);
      fetchBasureros();
    } catch (err) {
      console.error("Error al borrar basurero:", err);
    }
  };

  /* ---- Preparar edición ---- */
  const handleEdit = (b) => {
    setForm({
      nombre: b.nombre,
      usuario: b.usuario,
      email: b.email,
      password: "",
      rol: "BASURERO",
    });
    setEditingId(b.id);
    setEditMode(true);
    setShowModal(true);
  };

  /* ---- Abrir modal de asignar zona ---- */
  const openAssignModal = (basureroId) => {
    setAssignModalId(basureroId);
    setSelectedZone("");
  };

  /* ---- Realizar asignación de zona al basurero ---- */
  const confirmAssignZone = async () => {
    if (!selectedZone || !assignModalId) return;
    try {
      // Llamada al endpoint que inserta en usuario_zona
      await fetch(`${API}/usuario_zona`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          usuarioId: assignModalId,
          zonaId: selectedZone,
        }),
      });
      // Cerrar modal y recargar datos
      setAssignModalId(null);
      setSelectedZone("");
      fetchBasureros();
    } catch (err) {
      console.error("Error al asignar zona:", err);
      // Podrías mostrar un mensaje de error al usuario aquí si lo prefieres
    }
  };

  /* ---- UI ---- */
  return (
    <AnimatedPage>
      <section className="gestion-basureros">
        {/* ---------- Header ---------- */}
        <div className="header">
          <h2>🧹 Basureros</h2>
          <div style={{ display: "flex", gap: ".5rem" }}>
            <button className="refresh-btn" onClick={fetchBasureros}>
              ⟳ Refrescar
            </button>
            <button
              className="add-btn"
              onClick={() => {
                setEditMode(false);
                setForm({
                  nombre: "",
                  usuario: "",
                  email: "",
                  password: "",
                  rol: "BASURERO",
                });
                setShowModal(true);
              }}
            >
              + Añadir
            </button>
          </div>
        </div>

        {/* ---------- Buscador ---------- */}
        <div className="search-bar-container">
          <input
            type="text"
            className="search-bar"
            placeholder="🔍 Buscar por email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {/* ---------- Grid de tarjetas ---------- */}
        {isLoading ? (
          <div className="card-grid">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="skeleton" />
            ))}
          </div>
        ) : error ? (
          <p className="error">{error}</p>
        ) : (
          <div className="card-grid">
            {filtered.length === 0 ? (
              <p>No hay basureros registrados.</p>
            ) : (
              filtered.map((u) => (
                <div key={u.id} className="card">
                  <strong>{u.nombre}</strong>
                  <p>
                    <b>Usuario:</b> {u.usuario}
                  </p>
                  <p>
                    <b>Email:</b> {u.email}
                  </p>

                  {/* ---------- Acciones ---------- */}
                  <div className="card-actions">
                    <button
                      title="Eliminar"
                      onClick={() => setConfirmDeleteId(u.id)}
                    >
                      🗑️
                    </button>
                    <button title="Editar" onClick={() => handleEdit(u)}>
                      ✏️
                    </button>
                    <button
                      title="Asignar zona"
                      onClick={() => openAssignModal(u.id)}
                    >
                      🗺 Asignar zona
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* ---------- Modal Crear / Editar Basurero ---------- */}
        {showModal && (
          <div className="modal-overlay">
            <div className="modal">
              <h3>{editMode ? "Editar Basurero" : "Nuevo Basurero"}</h3>

              <form onSubmit={handleSubmit}>
                <input
                  name="nombre"
                  placeholder="Nombre"
                  value={form.nombre}
                  onChange={handleChange}
                  required
                />
                <input
                  name="usuario"
                  placeholder="Usuario"
                  value={form.usuario}
                  onChange={handleChange}
                  required
                />
                <input
                  name="email"
                  type="email"
                  placeholder="Email"
                  value={form.email}
                  onChange={handleChange}
                  required
                />
                {!editMode && (
                  <input
                    name="password"
                    type="password"
                    placeholder="Contraseña"
                    value={form.password}
                    onChange={handleChange}
                    required
                  />
                )}

                {error && <p className="error">{error}</p>}

                <div className="modal-buttons">
                  <button type="submit">{editMode ? "Guardar" : "Crear"}</button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowModal(false);
                      setEditMode(false);
                      setForm({
                        nombre: "",
                        usuario: "",
                        email: "",
                        password: "",
                        rol: "BASURERO",
                      });
                    }}
                  >
                    Cancelar
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* ---------- Confirmar borrado Basurero ---------- */}
        {confirmDeleteId && (
          <div className="modal-overlay">
            <div className="modal">
              <h3>¿Eliminar basurero?</h3>
              <p>Esta acción no se puede deshacer.</p>
              <div className="modal-buttons">
                <button onClick={confirmDelete}>Eliminar</button>
                <button onClick={() => setConfirmDeleteId(null)}>
                  Cancelar
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ---------- Modal Asignar Zona ---------- */}
        {assignModalId && (
          <div className="modal-overlay">
            <div className="modal">
              <h3>Asignar Zona al Basurero</h3>
              <p>Selecciona la zona a la que deseas asignar a este basurero:</p>

              <div className="assign-zone-container">
                <select
                  value={selectedZone}
                  onChange={(e) => setSelectedZone(e.target.value)}
                >
                  <option value="">– Elige una zona –</option>
                  {zonas.map((z) => (
                    <option key={z.id} value={z.id}>
                      {z.nombre}
                    </option>
                  ))}
                </select>
              </div>

              <div className="modal-buttons">
                <button onClick={confirmAssignZone} disabled={!selectedZone}>
                  🗺 Asignar
                </button>
                <button onClick={() => setAssignModalId(null)}>Cancelar</button>
              </div>
            </div>
          </div>
        )}
      </section>
    </AnimatedPage>
  );
}
