// src/map/MapaContenedores.jsx

import React, { useEffect, useMemo } from "react";
import { MapContainer, TileLayer, Polygon, Marker, Tooltip, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

// Importamos y “pegamos” AwesomeMarkers a L.AwesomeMarkers
import "leaflet.awesome-markers/dist/leaflet.awesome-markers.css";
import "leaflet.awesome-markers";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require("leaflet/dist/images/marker-icon-2x.png"),
  iconUrl:       require("leaflet/dist/images/marker-icon.png"),
  shadowUrl:     require("leaflet/dist/images/marker-shadow.png"),
});

function FitToZonas({ zonas }) {
  const map = useMap();
  const bounds = useMemo(() => {
    if (!zonas.length) return null;
    const puntos = zonas.flatMap((z) => z.geom);
    return L.latLngBounds(puntos.map(([lat, lon]) => [lat, lon]));
  }, [zonas]);

  useEffect(() => {
    if (bounds) {
      map.fitBounds(bounds, { padding: [20, 20] });
    }
  }, [bounds, map]);

  return null;
}

const iconPorEstado = (c) => {
  let markerColor;
  if (c.bloqueo) {
    markerColor = "gray";
  } else if (c.lleno) {
    markerColor = "red";
  } else if (c.carga_actual >= 0.75 * c.capacidad_maxima) {
    markerColor = "orange";
  } else {
    markerColor = "green";
  }

  return L.AwesomeMarkers.icon({
    icon:        "cube",
    markerColor,             
    prefix:      "fa",
    iconColor:   "white",
    extraClasses:""
  });
};

export default function MapaContenedores({
  contenedores = [],
  zonas        = [],
  placing = false,      // ← prop que indica si estamos en “modo colocar”
  onMarkerClick,
  onZonaClick,
  onMapClick,           // ← prop que dispara handleMapClickToPlace(e)
}) {
	
	useEffect(() => {
	  const mapEl = document.querySelector(".leaflet-container");
	  if (!mapEl) return;
	  mapEl.style.cursor = placing ? "crosshair" : "";
	}, [placing]);

  // Contamos cuántos contenedores hay en cada zona (para colorear polígonos)
  const countByZona = zonas.reduce((acc, z) => {
    acc[z.id] = 0;
    return acc;
  }, {});
  contenedores.forEach((c) => {
    if (c.zona?.id != null && countByZona[c.zona.id] != null) {
      countByZona[c.zona.id]++;
    }
  });

  const colorPorCount = (cnt) => {
    if (cnt <= 2)    return "#10b981";
    if (cnt <= 5)    return "#f59e0b";
    return "#ef4444";
  };

  return (
    <MapContainer
      center={[37.389, -5.984]}
      zoom={12}
      scrollWheelZoom
      style={{ height: "600px", width: "100%", borderRadius: "10px" }}
      whenCreated={(map) => {
        map.on("click", (e) => {
          if (onMapClick) onMapClick(e);
        });
      }}
    >
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution="© OpenStreetMap"
      />

      {zonas.map((z) => (
        <Polygon
          key={z.id}
          positions={z.geom} 
          pathOptions={{
            color:       colorPorCount(countByZona[z.id]),
            fillColor:   colorPorCount(countByZona[z.id]),
            fillOpacity: 0.3,
            weight:      2,
          }}
          eventHandlers={{
            click: (e) => {
              if (placing) {
                // ────────────────────────────────────────────────────────────────────────
                // Si estamos en “modo colocar”, en lugar de abrir el sidebar, 
                // forzamos manualmente la llamada a onMapClick(e) para que 
                // handleMapClickToPlace se ejecute y abra el modal:
                if (onMapClick) {
                  onMapClick(e);
                }
                // Luego, ya salimos sin hacer nada más:
                return;
                // ────────────────────────────────────────────────────────────────────────
              }

              // Si no estamos en modo “colocar”, abrimos el sidebar:
              if (onZonaClick) onZonaClick(z);

              // Y detenemos la propagación del clic para que no
              // llegue a ser tratado como “clic en el mapa”:
              if (e.originalEvent) {
                L.DomEvent.stopPropagation(e);
                e.originalEvent.stopPropagation();
              }
            },
          }}
        >
          <Tooltip sticky>
            <strong>{z.nombre}</strong>
            <br />
            {countByZona[z.id]} contenedores
          </Tooltip>
        </Polygon>
      ))}

      {contenedores.map((c) => {
        if (typeof c.lat === "number" && typeof c.lon === "number") {
          return (
            <Marker
              key={c.id}
              position={[c.lat, c.lon]}
              icon={iconPorEstado(c)}
              eventHandlers={{
                click: () => {
                  if (onMarkerClick) onMarkerClick(c);
                },
              }}
            >
              <Tooltip direction="top" offset={[0, -12]}>
                {c.nombre}
              </Tooltip>
            </Marker>
          );
        }
        return null;
      })}

      <FitToZonas zonas={zonas} />
    </MapContainer>
  );
}
