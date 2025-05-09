// src/consumidor/ConsumidorProfile.js
import React from "react";
import "../static/css/consumidor/consumidorProfile.css";

export default function ConsumidorProfile() {
  const email = localStorage.getItem("email");
  const rol = localStorage.getItem("rol");

  return (
    <div className="consumidor-profile">
      <h1>👤 Perfil del Consumidor</h1>
      <p><strong>Rol:</strong> {rol}</p>
      <p><strong>Email:</strong> {email || "consumidor@hacforchange.com"}</p>
      <p>Desde aquí podrás navegar entre las secciones del sistema utilizando el menú superior.</p>
    </div>
  );
}
