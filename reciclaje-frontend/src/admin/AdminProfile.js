import React from "react";
import "../static/css/admin/adminProfile.css";

export default function AdminProfile() {
  const email = localStorage.getItem("email"); // Si lo guardaste al loguearte
  const rol = localStorage.getItem("rol");

  return (
    <div className="admin-profile">
      <h1>👤 Perfil del Administrador</h1>
      <p><strong>Rol:</strong> {rol}</p>
      <p><strong>Email:</strong> {email || "admin@hacforchange.com"}</p>
      <p>Desde aquí podrás navegar entre las secciones del sistema utilizando el menú superior.</p>
    </div>
  );
}
