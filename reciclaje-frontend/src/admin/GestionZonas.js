import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { FaMapMarkerAlt, FaTrash, FaEdit } from "react-icons/fa";
import "../static/css/admin/gestionProveedores.css";
import "../static/css/admin/gestionZonas.css";
import AnimatedPage from "../components/AnimatedPage";

export default function GestionZonas() {
  const [zonas, setZonas] = useState([]);
  const [search, setSearch] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);
  const [zonaExpandida, setZonaExpandida] = useState(null); // <- Aquí controlas la tarjeta expandida
  const [form, setForm] = useState({ nombre: "", canal_mqtt: "" });
  const [error, setError] = useState("");
  const token = localStorage.getItem("token");

  const fetchZonas = () => {
    fetch("http://localhost:8080/api/zonas/with-contenedores", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => setZonas(data.zonas))
      .catch((err) => console.error("❌ Error al obtener zonas:", err));
  };

  useEffect(() => {
    fetchZonas();
  }, []);

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const url = editMode
      ? `http://localhost:8080/api/zonas/${editingId}`
      : "http://localhost:8080/api/zonas";
    const method = editMode ? "PUT" : "POST";

    const res = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(form),
    });

    if (res.ok) {
      fetchZonas();
      setForm({ nombre: "", canal_mqtt: "" });
      setShowModal(false);
      setEditMode(false);
      setEditingId(null);
    } else {
      setError("❌ Error al guardar zona");
    }
  };

  const confirmDelete = async () => {
    await fetch(`http://localhost:8080/api/zonas/${confirmDeleteId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    setConfirmDeleteId(null);
    fetchZonas();
  };

  const handleEdit = (zona) => {
    setForm({ nombre: zona.nombre, canal_mqtt: zona.canal_mqtt });
    setEditingId(zona.id);
    setEditMode(true);
    setShowModal(true);
  };

  const filtered = zonas.filter((z) =>
    z.nombre.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <AnimatedPage>
      <div className="gestion-proveedores">
        <div className="header">
          <h2>🗺️ Zonas de Recogida</h2>
          <button className="add-btn" onClick={() => {
            setEditMode(false);
            setForm({ nombre: "", canal_mqtt: "" });
            setShowModal(true);
          }}>+ Añadir</button>
        </div>

        <div className="search-bar-container">
          <input
            type="text"
            className="search-bar"
            placeholder="🔍 Buscar por nombre..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className="card-grid">
          {filtered.length === 0 ? (
            <p>No hay zonas registradas.</p>
          ) : (
            filtered.map((z) => {
		      const total = (z.contenedores || []).length;
			  const llenos = (z.contenedores || []).filter(c => c.lleno).length;
			  const bloqueados = (z.contenedores || []).filter(c => c.bloqueo).length;
              const isExpanded = zonaExpandida === z.id;

              return (
                <motion.div
                  key={z.id}
                  className="zona-card"
                  onClick={() => setZonaExpandida(isExpanded ? null : z.id)} // <- Aquí se controla expansión única
                  whileHover={{ scale: 1.02 }}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.3 }}
                >
                  <div className="zona-card-header">
                    <FaMapMarkerAlt size={26} />
                    <div>
                      <h3>{z.nombre}</h3>
                      <small>{z.canal_mqtt}</small>
                    </div>
                  </div>

                  <div className="zona-card-summary">
                    <p><b>Total:</b> {total} contenedores</p>
                    {llenos > 0 && <p style={{ color: "#dc2626" }}>❗ {llenos} llenos</p>}
                    {bloqueados > 0 && <p style={{ color: "gray" }}>🔒 {bloqueados} bloqueados</p>}
                  </div>

                  {isExpanded && (
                    <div className="zona-card-expand">
                      <strong>Contenedores:</strong>
					  {Array.isArray(z.contenedores) && z.contenedores.map(c => (
					    <div key={c.id} className="contenedor-detalle">
					      🔹 <b>{c.nombre}</b> — {c.lleno ? "❗ Lleno" : "✅ Disponible"} {c.bloqueo && "🔒 Bloqueado"}
					    </div>
					  ))}
                    </div>
                  )}

                  <div className="zona-card-actions">
                    <button onClick={(e) => { e.stopPropagation(); setConfirmDeleteId(z.id); }} title="Eliminar"><FaTrash /></button>
                    <button onClick={(e) => { e.stopPropagation(); handleEdit(z); }} title="Editar"><FaEdit /></button>
                  </div>
                </motion.div>
              );
            })
          )}
        </div>

        {/* MODAL CREAR/EDITAR */}
        {showModal && (
          <div className="modal-overlay">
            <div className="modal">
              <h3>{editMode ? "Editar Zona" : "Nueva Zona"}</h3>
              <form onSubmit={handleSubmit}>
                <input name="nombre" placeholder="Nombre" value={form.nombre} onChange={handleChange} required />
                <input name="canal_mqtt" placeholder="Canal MQTT" value={form.canal_mqtt} onChange={handleChange} required />
                {error && <p className="error">{error}</p>}
                <div className="modal-buttons">
                  <button type="submit">{editMode ? "Guardar" : "Crear"}</button>
                  <button type="button" onClick={() => {
                    setShowModal(false);
                    setEditMode(false);
                    setForm({ nombre: "", canal_mqtt: "" });
                  }}>Cancelar</button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* MODAL CONFIRMACIÓN */}
        {confirmDeleteId && (
          <div className="modal-overlay">
            <div className="modal">
              <h3>¿Eliminar zona?</h3>
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
