import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./auth/login/Login";
import Register from "./auth/register/Register";
import Home from "./home/Home";
import ProtectedRoute from "./components/ProtectedRoute";

import AdminDashboard from "./admin/AdminDashboard";
import AdminProfile from "./admin/AdminProfile";
import GestionAdministradores from "./admin/GestionAdministradores";
import GestionBasureros from "./admin/GestionBasureros";
import GestionConsumidores from "./admin/GestionConsumidores";
import GestionContenedores from "./admin/GestionContenedores";
import GestionProveedores from "./admin/GestionProveedores";
import GestionZonas from "./admin/GestionZonas";


import ProveedorDashboard from "./proveedor/ProveedorDashboard";
import VistaProductosProveedor from "./proveedor/VistaProductosProveedor";
import PerfilConsumidor from "./consumidor/PerfilConsumidor";
import BasureroDashboard from "./basurero/BasureroDashboard";

function App() {
  const rol = localStorage.getItem("rol");

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/home" element={
          <ProtectedRoute><Home /></ProtectedRoute>
        } />

        {/* Rutas dinámicas por rol */}
        {rol === "ADMINISTRADOR" && (
          <Route path="/admin" element={
            <ProtectedRoute><AdminDashboard /></ProtectedRoute>
          }>
            <Route index element={<AdminProfile />} />
            <Route path="usuarios" element={<GestionAdministradores />} />
            <Route path="proveedores" element={<GestionProveedores />} />
            <Route path="consumidores" element={<GestionConsumidores />} />
            <Route path="basureros" element={<GestionBasureros />} />
            <Route path="contenedores" element={<GestionContenedores />} />
			<Route path="zonas" element={<GestionZonas />} />
          </Route>
        )}

		{rol === "PROVEEDOR" && (
		  <Route path="/proveedor" element={
		    <ProtectedRoute><ProveedorDashboard /></ProtectedRoute>
		  }>
		    <Route index element={<VistaProductosProveedor />} />
		  </Route>
		)}

        {rol === "CONSUMIDOR" && (
          <Route path="/perfil-consumidor" element={
            <ProtectedRoute><PerfilConsumidor /></ProtectedRoute>
          } />
        )}
        {rol === "BASURERO" && (
          <Route path="/zona-basurero" element={
            <ProtectedRoute><BasureroDashboard /></ProtectedRoute>
          } />
        )}

        {/* Fallback si no hay coincidencias */}
        <Route path="*" element={<Navigate to="/home" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
