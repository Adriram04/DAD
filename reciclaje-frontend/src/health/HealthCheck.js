import React, { useState } from "react";

export default function HealthCheck() {
  const [status, setStatus] = useState(null);

  const checkBackend = async () => {
    try {
      // Cambia la URL si definiste el endpoint /health en otro lugar
      const response = await fetch("https://api.ecobins.tech/health", {
        method: "GET",
        mode: "cors",
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        // No fue 200 OK
        throw new Error(`Error de servidor: ${response.status}`);
      }

      const text = await response.text();
      setStatus(`OK: ${text}`);
    } catch (error) {
      setStatus(`Fallo al contactar el backend: ${error.message}`);
    }
  };

  return (
    <div style={{ padding: "2rem" }}>
      <h2>Prueba de conexión al Backend</h2>
      <button onClick={checkBackend}>Comprobar backend</button>
      {status && <p>{status}</p>}
    </div>
  );
}
