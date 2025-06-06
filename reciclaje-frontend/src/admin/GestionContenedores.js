// ───────────────────────────────────────────────────────────────
// src/admin/GestionContenedores.jsx
// ───────────────────────────────────────────────────────────────

import { useEffect, useState } from "react";
import { FaTimes, FaTrash, FaEdit, FaPlus } from "react-icons/fa";
import AnimatedPage from "../components/AnimatedPage";
import ModalPortal from "../components/ModalPortal";
import MapaContenedores from "../map/MapaContenedores";
import ZonaSidebar from "../components/ZonaSidebar";
import { motion, AnimatePresence } from "framer-motion";
import * as turf from "@turf/turf";

export default function GestionContenedores() {
  const [contenedores, setContenedores] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [zonas, setZonas] = useState([]);

  const [search, setSearch] = useState("");
  const [selectedContenedor, setSelectedContenedor] = useState(null);
  const [selectedZona, setSelectedZona] = useState(null);

  // ───── Para “modo colocar” ─────
  const [placing, setPlacing] = useState(false);

  /* Modal creación/edición */
  const [showForm, setShowForm] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editingId, setEditingId] = useState(null);

  // formData contendrá { nombre, zonaId, capacidad_maxima, lat, lon }
  const [formData, setFormData] = useState({
    nombre: "",
    zonaId: "",
    capacidad_maxima: "",
    lat: "",
    lon: "",
  });

  const token = localStorage.getItem("token");

  /* ─────────── Fetch inicial ─────────── */
  useEffect(() => {
    fetchContenedores();
    fetchZonasGeo();
    // eslint-disable-next-line
  }, []);

  const fetchContenedores = () => {
    fetch("https://api.ecobins.tech/contenedores", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => {
        setContenedores(d.contenedores || []);
        setFiltered(d.contenedores || []);
      })
      .catch((e) => console.error("❌ contenedores:", e));
  };

  const fetchZonasGeo = () => {
    fetch("https://api.ecobins.tech/zonas/geo", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setZonas(d.zonas || []))
      .catch((e) => console.error("❌ zonas geo:", e));
  };

  /* ─── Filtrar buscador ─── */
  useEffect(() => {
    const res = contenedores.filter(
      (c) =>
        c.nombre.toLowerCase().includes(search.toLowerCase()) ||
        c.zona.nombre.toLowerCase().includes(search.toLowerCase())
    );
    setFiltered(res);
  }, [search, contenedores]);

  /* ─── Actualizar formData al teclear en inputs ─── */
  const handleInput = (e) =>
    setFormData({ ...formData, [e.target.name]: e.target.value });

  /* ─── Guardar (POST o PUT) ─── */
  async function handleSubmit(e) {
    e.preventDefault();

    const url = editMode
      ? `https://api.ecobins.tech/contenedores/${editingId}`
      : "https://api.ecobins.tech/contenedores";
    const method = editMode ? "PUT" : "POST";

    const body = {
      nombre: formData.nombre,
      zonaId: Number(formData.zonaId),
      capacidad_maxima: Number(formData.capacidad_maxima),
      lat: Number(formData.lat),
      lon: Number(formData.lon),
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
      setEditMode(false);
      setFormData({
        nombre: "",
        zonaId: "",
        capacidad_maxima: "",
        lat: "",
        lon: "",
      });
    } else {
      alert("❌ Error al guardar contenedor");
    }
  }

  /* ─── Iniciar “modo colocar” ─── */
  const iniciarColocar = () => {
    setEditMode(false);
    setShowForm(false);
    setFormData({
      nombre: "",
      zonaId: "",
      capacidad_maxima: "",
      lat: "",
      lon: "",
    });
    setPlacing(true);
  };

  /* ─── Cuando el usuario clic en el mapa para “colocar” ─── */
  const handleMapClickToPlace = (e) => {
    if (!placing) return;

    const { lat, lng } = e.latlng;
    const punto = turf.point([lng, lat]);
    const zonaEncontrada = zonas.find((z) => {
      const coords = z.geom.map(([la, lo]) => [lo, la]);
      return turf.booleanPointInPolygon(punto, turf.polygon([coords]));
    });

    if (!zonaEncontrada) {
      alert("⚠ Ese punto está fuera de cualquier zona definida.");
      return;
    }

    setFormData((f) => ({
      ...f,
      lat: lat.toFixed(6),
      lon: lng.toFixed(6),
      zonaId: zonaEncontrada.id,
    }));

    setPlacing(false);
    setShowForm(true);
  };

  /* ─── Cancelar “modo colocar” con Esc ─── */
  useEffect(() => {
    const onKeyDown = (ev) => {
      if (placing && ev.key === "Escape") {
        setPlacing(false);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [placing]);

  /* ─── Borrar, editar, etc. ─── */
  const handleEdit = (c) => {
    setFormData({
      nombre: c.nombre,
      zonaId: c.zona.id,
      capacidad_maxima: c.capacidad_maxima || "",
      lat: c.lat || "",
      lon: c.lon || "",
    });
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
    if (selectedZona) {
      const zonaIdNum = selectedZona.id;
      const nuevos = filtered.filter(
        (x) => x.zona.id === zonaIdNum && x.id !== id
      );
      setSelectedZona({ ...selectedZona, contenedores: nuevos });
    }
  };

  return (
    <div className="contenedores-page">
      <AnimatedPage>
        <h2 style={{ margin: "1.5rem 0" }}>🗺 Mapa de Contenedores</h2>

        {/* ─── Barra buscador + botón “Nuevo” ─── */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "1rem",
            marginBottom: "1rem",
          }}
        >
          <input
            className="buscador"
            placeholder="🔍 Buscar contenedor o zona…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <button onClick={iniciarColocar}>
            <FaPlus /> Nuevo
          </button>
        </div>

        {/* ─── Aviso “modo colocar” ─── */}
        {placing && (
          <div
            style={{
              margin: "0 1rem 1rem",
              padding: "0.75rem 1rem",
              backgroundColor: "#fef3c7",
              border: "1px solid #fde68a",
              borderRadius: "8px",
              color: "#92400e",
              fontSize: "0.95rem",
            }}
          >
            ☝ Haz clic en el mapa para posicionar el contenedor. (Esc para
            cancelar)
          </div>
        )}

        {/* ─── MAPA ─── */}
        <div className="map-container">
          <MapaContenedores
            contenedores={filtered}
            zonas={zonas}
            placing={placing}
            onMarkerClick={setSelectedContenedor}
            onZonaClick={(z) => {
              if (placing) return;
              const contsEnEstaZona = filtered.filter(
                (c) => c.zona.id === z.id
              );
              setSelectedZona({ ...z, contenedores: contsEnEstaZona });
            }}
            onMapClick={handleMapClickToPlace}
          />
        </div>
      </AnimatedPage>

      {/* ─── Sidebar de zona ─── */}
      {selectedZona && (
        <ZonaSidebar
          zona={selectedZona}
          onClose={() => setSelectedZona(null)}
          onEditContenedor={handleEdit}
          onDeleteContenedor={handleDelete}
        />
      )}

      {/* ─── Modal de detalle contenedor (editar/borrar) ─── */}
      <AnimatePresence>
        {selectedContenedor && (
          <ModalPortal>
            <motion.div
              className="contenedores-modal"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              <motion.div
                className="contenedores-modal-contenido"
                initial={{ scale: 0.85, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.85, opacity: 0 }}
              >
                <button
                  className="cerrar"
                  onClick={() => setSelectedContenedor(null)}
                >
                  <FaTimes />
                </button>

                <h3>📦 {selectedContenedor.nombre}</h3>
                <p>
                  <b>Zona:</b> {selectedContenedor.zona.nombre}
                </p>
                <p>
                  <b>Lat:</b> {selectedContenedor.lat}
                </p>
                <p>
                  <b>Lon:</b> {selectedContenedor.lon}
                </p>
                <p>
                  <b>Capacidad:</b> {selectedContenedor.capacidad_maxima} kg
                </p>
                <p>
                  <b>Actual:</b> {selectedContenedor.carga_actual} kg
                </p>
                <p>
                  <b>¿Lleno?</b> {selectedContenedor.lleno ? "Sí" : "No"}
                </p>
                <p>
                  <b>¿Bloqueo?</b> {selectedContenedor.bloqueo ? "Sí" : "No"}
                </p>

                <div className="modal-acciones">
                  <button
                    className="editar"
                    onClick={() => handleEdit(selectedContenedor)}
                  >
                    <FaEdit /> Editar
                  </button>
                  <button
                    className="eliminar"
                    onClick={() => handleDelete(selectedContenedor.id)}
                  >
                    <FaTrash /> Eliminar
                  </button>
                </div>
              </motion.div>
            </motion.div>
          </ModalPortal>
        )}
      </AnimatePresence>

      {/* ─── Modal crear / editar contenedor ─── */}
      <AnimatePresence>
        {showForm && (
          <ModalPortal>
            <motion.div
              className="contenedores-modal"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              <motion.div
                className="contenedores-modal-contenido"
                initial={{ scale: 0.9 }}
                animate={{ scale: 1 }}
                exit={{ scale: 0.9 }}
              >
                <button
                  className="cerrar"
                  onClick={() => {
                    setShowForm(false);
                    setEditMode(false);
                  }}
                >
                  <FaTimes />
                </button>

                <h3>{editMode ? "Editar" : "Nuevo"} Contenedor</h3>

                <form onSubmit={handleSubmit}>
                  {/* ─── Nombre ─── */}
                  <input
                    name="nombre"
                    placeholder="Nombre"
                    value={formData.nombre}
                    onChange={handleInput}
                    required
                  />

                  {/* ─── Zona (ya preseleccionada) ─── */}
                  <select
                    name="zonaId"
                    value={formData.zonaId}
                    onChange={handleInput}
                    required
                  >
                    <option value="">Selecciona una zona</option>
                    {zonas.map((z) => (
                      <option key={z.id} value={z.id}>
                        {z.nombre}
                      </option>
                    ))}
                  </select>

                  {/* ─── Capacidad máxima ─── */}
                  <input
                    name="capacidad_maxima"
                    placeholder="Capacidad máxima (kg)"
                    type="number"
                    min="0"
                    value={formData.capacidad_maxima}
                    onChange={handleInput}
                    required
                  />

                  {/* ─── Latitud (rellenada) ─── */}
                  <input
                    name="lat"
                    placeholder="Latitud"
                    type="number"
                    step="any"
                    value={formData.lat}
                    onChange={handleInput}
                    required
                  />

                  {/* ─── Longitud (rellenada) ─── */}
                  <input
                    name="lon"
                    placeholder="Longitud"
                    type="number"
                    step="any"
                    value={formData.lon}
                    onChange={handleInput}
                    required
                  />

                  <div className="modal-acciones">
                    <button className="editar" type="submit">
                      {editMode ? "Guardar cambios" : "Crear"}
                    </button>
                  </div>
                </form>
              </motion.div>
            </motion.div>
          </ModalPortal>
        )}
      </AnimatePresence>
    </div>
  );
}
