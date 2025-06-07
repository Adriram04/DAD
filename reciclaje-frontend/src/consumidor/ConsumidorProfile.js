// src/consumidor/ConsumidorProfile.jsx

import React, { useEffect, useState } from "react";
import { motion } from "framer-motion";
import "./consumidorProfile.css";

// Importamos la imagen para que Webpack genere la URL correcta
import bannerImg from "../static/images/banner-consumidor.jpg";

export default function ConsumidorProfile() {
  const [usuario, setUsuario] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isAdding, setIsAdding] = useState(false);

  // Leemos token y userId guardados durante el login
  const token = localStorage.getItem("token");
  const userId = localStorage.getItem("userId");

  // … código existente …
  useLivePuntos({
    userId,
    onPuntos: (extra) =>
      setUsuario((u) => (u ? { ...u, puntos: u.puntos + extra } : u)),
  });

  useEffect(() => {
    // Si falta userId o token en localStorage, mostramos mensaje de “no sesión activa”
    if (!userId || !token) {
      setError("No se encontró sesión activa. Por favor, inicia sesión nuevamente.");
      setLoading(false);
      return;
    }

    // Petición al backend para obtener datos del perfil
    fetch(`https://api.ecobins.tech/usuarios/${userId}/perfil`, {
      headers: {
        "Content-Type": "application/json",
        // Si en el futuro proteges este endpoint con JWT:
        // Authorization: `Bearer ${token}`,
      },
    })
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((data) => {
        // data: { id, nombre, email, rol, puntos, card_uid }
        setUsuario(data);
      })
      .catch((err) => {
        console.error("Error al cargar perfil:", err);
        setError("No se pudo cargar tu perfil.");
      })
      .finally(() => {
        setLoading(false);
      });
  }, [userId, token]);

  // Función para solicitar enlace de Google Wallet
  const handleAddToWallet = () => {
    if (!userId || !token) return;

    setIsAdding(true);

    fetch(`https://api.ecobins.tech/wallet/link?userId=${userId}`, {
      headers: {
        "Content-Type": "application/json",
        // Si en el futuro proteges este endpoint con JWT:
        // Authorization: `Bearer ${token}`,
      },
    })
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((data) => {
        // data: { url: "https://pay.google.com/gp/v/save/…JWT…" }
        if (data.url) {
          window.location.href = data.url; // Redirige a Google Wallet
        } else {
          throw new Error("No se recibió la URL para Wallet");
        }
      })
      .catch((err) => {
        console.error("Error al generar Wallet link:", err);
        setError("No se pudo obtener el enlace para Google Wallet.");
        setIsAdding(false);
      });
  };

  // Estado “loading”
  if (loading) {
    return (
      <div className="consumidor-profile loading">
        <span>Cargando tu perfil…</span>
      </div>
    );
  }

  // Estado “error” o “no sesión”
  if (error) {
    return (
      <div className="consumidor-profile error">
        <p>{error}</p>
      </div>
    );
  }

  // Si no hay datos de usuario (caso borde)
  if (!usuario) {
    return null;
  }

  return (
    <motion.div
      className="consumidor-profile-wrapper"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* ───────── Banner en la parte superior ───────── */}
      <div
        className="profile-banner"
        style={{ backgroundImage: `url(${bannerImg})` }}
      />

      {/* ───────── Contenido principal ───────── */}
      <div className="profile-content-no-avatar">
        {/* Nombre y correo del usuario */}
        <motion.div
          className="profile-titulo"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.4 }}
        >
          <h2>{usuario.nombre}</h2>
          <p className="email-text">{usuario.email}</p>
        </motion.div>

        {/* ───────── Tarjetas de estadísticas ───────── */}
        <div className="stats-cards">
          <motion.div
            className="stat-card"
            whileHover={{ scale: 1.03 }}
            transition={{ type: "spring", stiffness: 150 }}
          >
            <h3>Puntos</h3>
            <p className="stat-value">{usuario.puntos}</p>
          </motion.div>

          {usuario.card_uid && (
            <motion.div
              className="stat-card"
              whileHover={{ scale: 1.03 }}
              transition={{ type: "spring", stiffness: 150 }}
            >
              <h3>Tarjeta UID</h3>
              <p className="stat-value">{usuario.card_uid}</p>
            </motion.div>
          )}
        </div>

        {/* ───────── Botón “Añadir a Google Wallet” ───────── */}
        <motion.div
          className="wallet-section"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.4 }}
        >
          <button
            className="wallet-button"
            onClick={handleAddToWallet}
            disabled={isAdding}
          >
            {isAdding ? "Preparando Wallet…" : "➕ Añadir a Google Wallet"}
          </button>
        </motion.div>

        {/* ───────── Nota explicativa debajo del botón ───────── */}
        {!isAdding && usuario.card_uid && (
          <motion.div
            className="wallet-note"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.7, duration: 0.3 }}
          >
            <p>
              Si tu dispositivo no abre Wallet automáticamente, pulsa el
              botón de arriba y sigue las indicaciones.
            </p>
          </motion.div>
        )}
      </div>
    </motion.div>
  );
}
