import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Landing from "./intro/Landing";
import Login from "./auth/login/Login";
import Register from "./auth/register/Register";
import Home from "./home/Home";
import ProtectedRoute from "./components/ProtectedRoute";
import HealthCheck from "./health/HealthCheck";

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

import ConsumidorProfile from "./consumidor/ConsumidorProfile";
import ConsumidorDashboard from "./consumidor/ConsuidorDashboard";

import BasureroDashboard from "./basurero/BasureroDashboard";
import GestionContenedoresBasureros from "./basurero/GestionContenedoresBasureros";
import GestionZonasBasureros from "./basurero/GestionZonasBasureros";

function App() {
  const rol = localStorage.getItem("rol");

  return (
    <BrowserRouter>
      <Routes>
	  {/* Página de aterrizaje con logo, 3D y welcome+login */}
	      <Route path="/" element={<Landing />} />
	
	      {/* Autenticación y verificación */}
	      <Route path="/login"        element={<Login />} />
	      <Route path="/register"     element={<Register />} />
	      <Route path="/health-check" element={<HealthCheck />} />
	
	      {/* Home (redirección interna tras login) */}
	      <Route
	        path="/home"
	        element={
	          <ProtectedRoute>
	            <Home />
	          </ProtectedRoute>
	        }
	      />

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
          <Route path="/consumidor" element={
            <ProtectedRoute><ConsumidorDashboard /></ProtectedRoute>
          }/>
        )}
        {rol === "BASURERO" && (
          <Route path="/basurero" element={
            <ProtectedRoute><BasureroDashboard /></ProtectedRoute>
          }>
		  	<Route path="contenedores" element={<GestionContenedoresBasureros />} />
			<Route path="zonas" element={<GestionZonasBasureros />} />
			</Route>
        )}
        {/* Fallback si no hay coincidencias */}
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
