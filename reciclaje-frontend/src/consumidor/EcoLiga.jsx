import React, { useEffect, useState } from "react";
import AnimatedPage from "../components/AnimatedPage"; // Si no lo usas, puedes quitar esta línea
import "./EcoLiga.css"; // Importamos el CSS de esta vista

export default function EcoLiga() {
  const [ranking, setRanking] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch("https://api.ecobins.tech/usuarios/leaderboard?limit=50")
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((data) => {
        setRanking(data.usuarios || []);
      })
      .catch((err) => {
        console.error("Error al cargar EcoLiga:", err);
        setError("No se pudo cargar la EcoLiga");
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, []);

  return (
    <AnimatedPage> {/* Si no tienes AnimatedPage, puedes envolverlo en un <div> */}
      <section className="ecoliga-container">
        {/* Encabezado */}
        <header className="ecoliga-header">
          <div className="ecoliga-trophy">🏆</div>
          <h1 className="ecoliga-title">EcoLiga</h1>
        </header>

        {/* Estado de carga */}
        {isLoading && (
          <div className="ecoliga-loading">
            <span>Cargando EcoLiga…</span>
          </div>
        )}

        {/* Si hay error */}
        {error && (
          <div className="ecoliga-error">
            {error}
          </div>
        )}

        {/* Si no hay datos */}
        {!isLoading && !error && ranking.length === 0 && (
          <div className="ecoliga-empty">
            No hay consumidores con puntos aún.
          </div>
        )}

        {/* Lista de ranking */}
        {!isLoading && !error && ranking.length > 0 && (
          <ul className="ecoliga-list">
            {ranking.map((user, idx) => {
              // Determinamos clase y colores especiales para top 3
              let itemClass = "ecoliga-item";
              let medal = null;
              let rankClass = "ecoliga-rank";
              let nameClass = "ecoliga-name";
              let pointsClass = "ecoliga-points";

              if (idx === 0) {
                itemClass += " ecoliga-item-first";
                medal = <span className="ecoliga-medal ecoliga-medal-gold">🥇</span>;
                rankClass += " ecoliga-rank-gold";
                nameClass += " ecoliga-name-gold";
                pointsClass += " ecoliga-points-gold";
              } else if (idx === 1) {
                itemClass += " ecoliga-item-second";
                medal = <span className="ecoliga-medal ecoliga-medal-silver">🥈</span>;
                rankClass += " ecoliga-rank-silver";
                nameClass += " ecoliga-name-silver";
                pointsClass += " ecoliga-points-silver";
              } else if (idx === 2) {
                itemClass += " ecoliga-item-third";
                medal = <span className="ecoliga-medal ecoliga-medal-bronze">🥉</span>;
                rankClass += " ecoliga-rank-bronze";
                nameClass += " ecoliga-name-bronze";
                pointsClass += " ecoliga-points-bronze";
              }

              return (
                <li key={user.id} className={itemClass}>
                  <div className="ecoliga-left">
                    <div className={rankClass}>{idx + 1}</div>
                    {medal && <div className="ecoliga-medal-container">{medal}</div>}
                    <div className={nameClass}>{user.nombre}</div>
                  </div>
                  <div className={pointsClass}>{user.puntos} pts</div>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </AnimatedPage>
  );
}
