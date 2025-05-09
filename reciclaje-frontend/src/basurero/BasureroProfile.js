import React from "react";
import "../static/css/basurero/basureroProfile.css";

export default function BasureroProfile() {
  const email = localStorage.getItem("email");
  const rol = localStorage.getItem("rol");

  return (
    <div className="basurero-profile">
      <h1>👤 Perfil del Basurero</h1>
      <p><strong>Rol:</strong> {rol}</p>
      <p><strong>Email:</strong> {email || "basurero@hacforchange.com"}</p>
      <p>Desde aquí podrás navegar entre las secciones del sistema utilizando el menú superior.</p>
    </div>
  );
}