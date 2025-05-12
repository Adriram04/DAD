import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { FaMapMarkerAlt, FaTimes, FaTrash, FaEdit, FaPlus } from "react-icons/fa";
import mapaFondo from "../static/images/sevilla.png";
import "../static/css/admin/gestionContenedores.css";
import AnimatedPage from "../components/AnimatedPage";
import ModalPortal from "../components/ModalPortal";


export default function GestionContenedores() {
  const [contenedores, setContenedores] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [search, setSearch] = useState("");
  const [selectedContenedor, setSelectedContenedor] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState({ nombre: "", zonaId: "", capacidad_maxima: "" });
  const [editingId, setEditingId] = useState(null);
  const [zonas, setZonas] = useState([]);
  const token = localStorage.getItem("token");

  const fetchContenedores = () => {
    fetch("https://api.ecobins.tech/contenedores", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => {
        setContenedores(data.contenedores);
        setFiltered(data.contenedores);
      })
      .catch((err) => console.error("❌ Error al obtener contenedores:", err));
  };

  const fetchZonas = () => {
    fetch("https://api.ecobins.tech/zonas", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => setZonas(data.zonas || []))
      .catch((err) => console.error("❌ Error al obtener zonas:", err));
  };

  useEffect(() => {
    fetchContenedores();
    fetchZonas();
  }, []);

  useEffect(() => {
    const results = contenedores.filter((c) =>
      c.nombre.toLowerCase().includes(search.toLowerCase()) ||
      c.zona.nombre.toLowerCase().includes(search.toLowerCase())
    );
    setFiltered(results);
  }, [search, contenedores]);

  const handleInputChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const url = editMode
      ? `https://api.ecobins.tech/contenedores/${editingId}`
      : "https://api.ecobins.tech/contenedores";
    const method = editMode ? "PUT" : "POST";

    const body = {
      nombre: formData.nombre,
      zonaId: parseInt(formData.zonaId, 10),
      capacidad_maxima: parseFloat(formData.capacidad_maxima),
    };

    const res = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    });

    if (res.ok) {
      fetchContenedores();
      setShowForm(false);
      setFormData({ nombre: "", zonaId: "", capacidad_maxima: "" });
      setEditMode(false);
      setEditingId(null);
    } else {
      alert("❌ Error al guardar contenedor");
    }
  };

  const handleEdit = (c) => {
    setFormData({ nombre: c.nombre, zonaId: c.zona.id, capacidad_maxima: c.capacidad_maxima || "" });
    setEditingId(c.id);
    setEditMode(true);
    setShowForm(true);
    setSelectedContenedor(null);
  };

  const handleDelete = async (id) => {
    if (!window.confirm("¿Eliminar contenedor permanentemente?")) return;
    await fetch(`https://api.ecobins.tech/contenedores/${id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    fetchContenedores();
    setSelectedContenedor(null);
  };

  return (
    <>
      <AnimatedPage>
        <div className="mapa-contenedores">
          <h2>🗺️ Mapa de Contenedores</h2>

          <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <input
              type="text"
              placeholder="🔍 Buscar contenedor o zona..."
              value={search}
              className="buscador"
              onChange={(e) => setSearch(e.target.value)}
            />
            <button
              onClick={() => {
                setShowForm(true);
                setEditMode(false);
                setFormData({ nombre: "", zonaId: "", capacidad_maxima: "" });
              }}
            >
              <FaPlus /> Nuevo
            </button>
          </div>

          <div className="mapa-fondo" style={{ backgroundImage: `url(${mapaFondo})` }}>
            <AnimatePresence>
              {filtered.map((c) => (
                <motion.div
                  key={c.id}
                  className="marcador"
                  style={{
                    top: `${(c.id * 37) % 80 + 5}%`,
                    left: `${(c.id * 61) % 85 + 5}%`,
                  }}
                  onClick={() => setSelectedContenedor(c)}
                >
                  <FaMapMarkerAlt
                    className="icono-marcador"
                    style={{ color: c.bloqueo ? "gray" : "red" }}
                  />
                  {c.lleno && <div className="alerta">❗</div>}
                  <span className="tooltip">{c.nombre}</span>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        </div>
      </AnimatedPage>

      {/* Modal de visualización */}
      <AnimatePresence>
        {selectedContenedor && (
          <motion.div className="contenedores-modal" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <motion.div
              className="contenedores-modal-contenido"
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.8, opacity: 0 }}
              transition={{ duration: 0.3 }}
            >
              <button className="cerrar" onClick={() => setSelectedContenedor(null)}><FaTimes /></button>
              <h3>📦 {selectedContenedor.nombre}</h3>
              <p><strong>Zona:</strong> {selectedContenedor.zona.nombre}</p>
              <p><strong>Capacidad:</strong> {selectedContenedor.capacidad_maxima} kg</p>
              <p><strong>Actual:</strong> {selectedContenedor.carga_actual} kg</p>
              <p><strong>¿Lleno?</strong> {selectedContenedor.lleno ? "Sí" : "No"}</p>
              <p><strong>¿Bloqueo?</strong> {selectedContenedor.bloqueo ? "Sí" : "No"}</p>
              <div className="modal-acciones">
                <button className="editar" onClick={() => handleEdit(selectedContenedor)}><FaEdit /> Editar</button>
                <button className="eliminar" onClick={() => handleDelete(selectedContenedor.id)}><FaTrash /> Eliminar</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Modal de creación/edición */}
      <AnimatePresence>
        {showForm && (
          <ModalPortal>
		  	<motion.div className="contenedores-modal" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
	          <motion.div className="contenedores-modal-contenido" initial={{ scale: 0.9 }} animate={{ scale: 1 }} exit={{ scale: 0.9 }}>
	            <button className="cerrar" onClick={() => {
	              setShowForm(false);
	              setEditMode(false);
	              setEditingId(null);
	            }}>
	              <FaTimes />
	            </button>
	            <h3>{editMode ? "Editar Contenedor" : "Nuevo Contenedor"}</h3>
	            <form onSubmit={handleSubmit}>
	              <input name="nombre" placeholder="Nombre" value={formData.nombre} onChange={handleInputChange} required />
	              <select name="zonaId" value={formData.zonaId} onChange={handleInputChange} required>
	                <option value="">Selecciona una zona</option>
	                {zonas.map((z) => (
	                  <option key={z.id} value={z.id}>{z.nombre}</option>
	                ))}
	              </select>
	              <input
	                name="capacidad_maxima"
	                placeholder="Capacidad máxima (kg)"
	                type="number"
	                min="0"
	                value={formData.capacidad_maxima}
	                onChange={handleInputChange}
	                required
	              />
	              <div className="modal-acciones">
	                <button className="editar" type="submit">{editMode ? "Guardar cambios" : "Crear"}</button>
	              </div>
	            </form>
	          </motion.div>
	        </motion.div>
		  </ModalPortal>
        )}
      </AnimatePresence>
    </>
  );
}
