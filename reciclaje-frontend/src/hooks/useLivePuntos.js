// src/hooks/useLivePuntos.js
import { useEffect } from "react";
import mqtt from "mqtt/dist/mqtt";     // versión browser

export default function useLivePuntos({ userId, onPuntos }) {
  useEffect(() => {
    if (!userId) return;
    const urlWS  = "wss://api.ecobins.tech:8083/mqtt"; // ⇦ endpoint WS de tu broker
    const client = mqtt.connect(urlWS, { clean: true });

    client.on("connect", () => {
      client.subscribe(`ui/usuarios/${userId}/puntos`, { qos: 1 });
    });

    client.on("message", (topic, payload) => {
      try {
        const { puntos } = JSON.parse(payload.toString());
        onPuntos(puntos);
      } catch (e) {
        console.error("payload puntos malformado:", e);
      }
    });

    return () => client.end(true);
  }, [userId, onPuntos]);
}
