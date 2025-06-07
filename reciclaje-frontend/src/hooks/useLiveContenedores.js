// src/hooks/useLiveContenedores.js
import { useEffect } from "react";

export default function useLiveContenedores({
  setContenedores,
  token,
  interval = 20000,           // 20 s por defecto
}) {
  useEffect(() => {
    let cancelado = false;

    async function fetchContenedores() {
      try {
        const res = await fetch("https://api.ecobins.tech/contenedores", {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();              // { contenedores:[…] }
        if (!cancelado) setContenedores(data.contenedores || []);
      } catch (err) {
        console.error("❌ contenedores:", err);
      }
    }

    fetchContenedores();                 // llamada inicial
    const id = setInterval(fetchContenedores, interval);
    return () => {
      cancelado = true;
      clearInterval(id);
    };
  }, [token, setContenedores, interval]);
}
