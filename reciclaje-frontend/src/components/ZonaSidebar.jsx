// src/components/ZonaSidebar.jsx

import React from "react";
import { FaTimes } from "react-icons/fa";
import "../static/css/components/zonaSidebar.css";
import { colorPorEstado } from "../map/utils"; // Importamos la función centralizada

/**
 * Props:
 * - zona: {
 *     id,
 *     nombre,
 *     geom,
 *     contenedores: [
 *       { 
 *         id, 
 *         nombre, 
 *         capacidad_maxima, 
 *         carga_actual, 
 *         lleno, 
 *         bloqueo, 
 *         lat, 
 *         lon, 
 *         zona: { ... } 
 *       }
 *     ]
 *   }
 * - onClose: función callback para cerrar el panel
 * - onEditContenedor: callback(c) para editar un contenedor
 * - onDeleteContenedor: callback(id) para eliminar un contenedor
 */
export default function ZonaSidebar({
  zona,
  onClose,
  onEditContenedor,
  onDeleteContenedor,
}) {
  return (
    <div className="zona-sidebar-overlay">
      <div className="zona-sidebar">
        <div className="sidebar-header">
          <h3>📍 {zona.nombre}</h3>
          <button className="cerrar-sidebar" onClick={onClose}>
            <FaTimes />
          </button>
        </div>

        <div className="zona-info">
          <p>
            <strong>Total de contenedores:</strong> {zona.contenedores.length}
          </p>
          <ul className="lista-contenedores">
            {zona.contenedores.map((c) => {
              const color = colorPorEstado(c);

              return (
                <li key={c.id} className="contenedor-item">
                  <button
                    className="btn-contenedor"
                    style={{ background: color }}
                    onClick={() => onEditContenedor(c)}
                  >
                    {c.nombre}
                  </button>
                  <button
                    className="btn-delete"
                    onClick={() => onDeleteContenedor(c.id)}
                    title="Eliminar contenedor"
                  >
                    🗑️
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      </div>
    </div>
  );
}
