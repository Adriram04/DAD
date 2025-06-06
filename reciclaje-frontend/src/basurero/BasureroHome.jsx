// src/basurero/BasureroContenedores.jsx

import { useEffect, useState } from "react";
import { FaTimes, FaTrash, FaEdit, FaPlus } from "react-icons/fa";
import AnimatedPage from "../components/AnimatedPage";
import ModalPortal from "../components/ModalPortal";
import MapaContenedores from "../map/MapaContenedores";
import ZonaSidebar from "../components/ZonaSidebar";
import { motion, AnimatePresence } from "framer-motion";
import * as turf from "@turf/turf";
import "../static/css/basurero/basureroHome.css";

const API = "https://api.ecobins.tech";

export default function BasureroContenedores() {
  // Asumimos que al hacer login guardas el ID en localStorage
  const basureroId = localStorage.getItem("userId");
  const token = localStorage.getItem("token");

  /* ───── estados ───── */
  const [todasZonas, setTodasZonas] = useState([]);      // toda la lista de zonas (geometría)
  const [misZonas, setMisZonas] = useState([]);          // solo las zonas asignadas a este basurero
  const [contenedores, setContenedores] = useState([]);  // todos los contenedores
  const [filteredCont, setFilteredCont] = useState([]);  // contenedores que pertenezcan a misZonas y estén llenos/bloqueados

  const [search, setSearch] = useState("");
  const [selectedContenedor, setSelectedContenedor] = useState(null);
  const [selectedZona, setSelectedZona] = useState(null);

  // ───── Para “modo colocar” (crear nuevo contenedor) ─────
  const [placing, setPlacing] = useState(false);

  /* Modal crear/editar */
  const [showForm, setShowForm] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    nombre: "",
    zonaId: "",
    capacidad_maxima: "",
    lat: "",
    lon: "",
  });

  /* ───── Fetch inicial ───── */
  useEffect(() => {
    fetchTodasZonasGeo();
    fetchMisZonas();
    fetchContenedores();
    // eslint-disable-next-line
  }, []);

  // 1) Traer todas las zonas (para geometrías)
  const fetchTodasZonasGeo = () => {
    fetch(`${API}/zonas/geo`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((d) => setTodasZonas(d.zonas || []))
      .catch((e) => console.error("❌ Error cargando todas las zonas geo:", e));
  };

  // 2) Traer las zonas asignadas a este basurero (solo IDs y nombres y geom)
  const fetchMisZonas = () => {
    fetch(`${API}/basurero/${basureroId}/zonas`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((d) => setMisZonas(d.zonas || []))
      .catch((e) =>
        console.error("❌ Error cargando zonas asignadas al basurero:", e)
      );
  };

  // 3) Traer todos los contenedores (luego filtramos)
  const fetchContenedores = () => {
    fetch(`${API}/contenedores`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((d) => {
        setContenedores(d.contenedores || []);
        setFilteredCont(
          (d.contenedores || []).filter((c) =>
            // Filtramos contenedores que pertenezcan a las zonas asignadas Y que estén llenos o bloqueados
            d.zonas
              ? (d.zonas.map((z) => z.id).includes(c.zona.id) &&
                  (c.bloqueo === true || c.lleno === true))
              : false
          )
        );
      })
      .catch((e) => console.error("❌ Error cargando contenedores:", e));
  };

  // 4) Cuando cambian misZonas, volvemos a filtrar contenedores
  useEffect(() => {
    const zonaIds = new Set(misZonas.map((z) => z.id));
    setFilteredCont(
      contenedores.filter(
        (c) =>
          zonaIds.has(c.zona.id) && (c.bloqueo === true || c.lleno === true)
      )
    );
    // eslint-disable-next-line
  }, [misZonas, contenedores]);

  /* ───── Helpers de formulario ───── */
  const handleInput = (e) =>
    setFormData({ ...formData, [e.target.name]: e.target.value });

  /* ───── Crear / Editar contenedor ───── */
  async function handleSubmit(e) {
    e.preventDefault();
    const url = editMode
      ? `${API}/contenedores/${editingId}`
      : `${API}/contenedores`;
    const method = editMode ? "PUT" : "POST";
    const body = {
      nombre: formData.nombre,
      zonaId: Number(formData.zonaId),
      capacidad_maxima: Number(formData.capacidad_maxima),
      lat: Number(formData.lat),
      lon: Number(formData.lon),
    };

    try {
      const res = await fetch(url, {
        method,
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(body),
      });
      if (!res.ok) throw new Error(await res.text());
      await fetchContenedores();
      setShowForm(false);
      setEditMode(false);
      setFormData({
        nombre: "",
        zonaId: "",
        capacidad_maxima: "",
        lat: "",
        lon: "",
      });
    } catch (err) {
      alert("❌ Error al guardar contenedor: " + err.message);
    }
  }

  /* ───── Iniciar “modo colocar” ───── */
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

  /* ───── Click en mapa (modo colocar) ───── */
  const handleMapClickToPlace = (e) => {
    if (!placing) return;
    const { lat, lng } = e.latlng;
    const punto = turf.point([lng, lat]);

    // Buscamos qué polígono (de todasZonas) contiene este punto, PERO solo si esa zona está en misZonas
    const zonaEncontrada = todasZonas.find((z) => {
      if (!misZonas.find((mz) => mz.id === z.id)) return false;
      const coords = z.geom.map(([la, lo]) => [lo, la]); // invertimos a [lon, lat]
      return turf.booleanPointInPolygon(punto, turf.polygon([coords]));
    });

    if (!zonaEncontrada) {
      alert("⚠️ Ese punto está fuera de tus zonas asignadas.");
      return;
    }

    // 1) Prellenamos formData con lat/lon/zonaId
    setFormData((f) => ({
      ...f,
      lat: lat.toFixed(6),
      lon: lng.toFixed(6),
      zonaId: zonaEncontrada.id,
    }));
    // 2) Salimos de colocar
    setPlacing(false);
    // 3) Abrimos modal de crear
    setShowForm(true);
  };

  /* ───── Cancelar modo colocar con Escape ───── */
  useEffect(() => {
    const onKeyDown = (ev) => {
      if (placing && ev.key === "Escape") {
        setPlacing(false);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [placing]);

  /* ───── Preparar edición ───── */
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

  /* ───── Borrar contenedor ───── */
  const handleDelete = async (id) => {
    if (!window.confirm("¿Eliminar contenedor permanentemente?")) return;
    try {
      await fetch(`${API}/contenedores/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      await fetchContenedores();
      setSelectedContenedor(null);
      if (selectedZona) {
        const zonaIdNum = selectedZona.id;
        const nuevos = filteredCont.filter(
          (x) => x.zona.id === zonaIdNum && x.id !== id
        );
        setSelectedZona({ ...selectedZona, contenedores: nuevos });
      }
    } catch (err) {
      console.error("Error borrando contenedor:", err);
    }
  };

  /* ───── Filtrar buscador ───── */
  useEffect(() => {
    const texto = search.toLowerCase();
    const res = filteredCont.filter(
      (c) =>
        c.nombre.toLowerCase().includes(texto) ||
        c.zona.nombre.toLowerCase().includes(texto)
    );
    setFilteredCont(res);
  }, [search]);

  return (
    <div className="contenedores-page">
      <AnimatedPage>
        <h2 style={{ margin: "1.5rem 0" }}>🗺️ Mapa de Contenedores</h2>

        {/* ─── Buscador + Botón Nuevo ─── */}
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
            ☝️ Haz clic en el mapa dentro de tus zonas asignadas. (Esc para
            cancelar)
          </div>
        )}

        {/* ─── MAPA ─── */}
        <div className="map-container">
          <MapaContenedores
            contenedores={filteredCont}
            zonas={misZonas}
            placing={placing}
            onMarkerClick={setSelectedContenedor}
            onZonaClick={(z) => {
              if (placing) return;
              const contsEnEstaZona = filteredCont.filter(
                (c) => c.zona.id === z.id
              );
              setSelectedZona({ ...z, contenedores: contsEnEstaZona });
            }}
            onMapClick={handleMapClickToPlace}
          />
        </div>
      </AnimatedPage>

      {/* ─── Sidebar de Zona ─── */}
      {selectedZona && (
        <ZonaSidebar
          zona={selectedZona}
          onClose={() => setSelectedZona(null)}
          onEditContenedor={handleEdit}
          onDeleteContenedor={handleDelete}
        />
      )}

      {/* ─── Modal Detalle Contenedor ─── */}
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

      {/* ─── Modal Crear/Editar Contenedor ─── */}
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
                  <input
                    name="nombre"
                    placeholder="Nombre"
                    value={formData.nombre}
                    onChange={handleInput}
                    required
                  />

                  <select
                    name="zonaId"
                    value={formData.zonaId}
                    onChange={handleInput}
                    required
                  >
                    <option value="">Selecciona una zona</option>
                    {misZonas.map((z) => (
                      <option key={z.id} value={z.id}>
                        {z.nombre}
                      </option>
                    ))}
                  </select>

                  <input
                    name="capacidad_maxima"
                    placeholder="Capacidad máxima (kg)"
                    type="number"
                    min="0"
                    value={formData.capacidad_maxima}
                    onChange={handleInput}
                    required
                  />

                  <input
                    name="lat"
                    placeholder="Latitud"
                    type="number"
                    step="any"
                    value={formData.lat}
                    onChange={handleInput}
                    required
                  />

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
