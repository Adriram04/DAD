import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Landing              from "./intro/Landing";
import Login                from "./auth/login/Login";
import Register             from "./auth/register/Register";
import Home                 from "./home/Home";
import ProtectedRoute       from "./components/ProtectedRoute";
import HealthCheck          from "./health/HealthCheck";
import NotFound 			from "./components/NotFound";

/* ADMIN */
import AdminDashboard       from "./admin/AdminDashboard";
import AdminProfile         from "./admin/AdminProfile";
import GestionBasureros     from "./admin/GestionBasureros";
import GestionConsumidores  from "./admin/GestionConsumidores";
import GestionContenedores  from "./admin/GestionContenedores";
import GestionProveedores   from "./admin/GestionProveedores";

/* PROVEEDOR */
import ProveedorDashboard   from "./proveedor/ProveedorDashboard";
import VistaProductosProveedor from "./proveedor/VistaProductosProveedor";

/* CONSUMIDOR */
import ConsumidorDashboard  from "./consumidor/ConsuidorDashboard";
import ConsumidorProfile    from "./consumidor/ConsumidorProfile";   // aún sin usar

/* BASURERO */
import BasureroDashboard    from "./basurero/BasureroDashboard";
import GestionContenedoresBasureros from "./basurero/GestionContenedoresBasureros";
import GestionZonasBasureros        from "./basurero/GestionZonasBasureros";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* públicas */}
        <Route path="/"            element={<Landing />}  />
        <Route path="/login"       element={<Login />}    />
        <Route path="/register"    element={<Register />} />
        <Route path="/health-check"element={<HealthCheck />} />

        {/* home genérico (elige dashboard) */}
        <Route
          path="/home"
          element={
            <ProtectedRoute>
              <Home />
            </ProtectedRoute>
          }
        />

        {/* ADMINISTRADOR */}
        <Route path="/admin/*" element={
          <ProtectedRoute roles={["ADMINISTRADOR"]}>
            <AdminDashboard />
          </ProtectedRoute>
        }>
          <Route index            element={<AdminProfile />}          />
          <Route path="proveedores" element={<GestionProveedores />}   />
          <Route path="consumidores" element={<GestionConsumidores />} />
          <Route path="basureros" element={<GestionBasureros />}      />
          <Route path="contenedores" element={<GestionContenedores />} />
		  
		  {/* catch-all SOLO para el dashboard admin */}
		  <Route path="*" element={<NotFound />} />
        </Route>

        {/* PROVEEDOR */}
        <Route path="/proveedor/*" element={
          <ProtectedRoute roles={["PROVEEDOR"]}>
            <ProveedorDashboard />
          </ProtectedRoute>
        }>
          <Route index element={<VistaProductosProveedor />} />
        </Route>

        {/* CONSUMIDOR */}
        <Route path="/consumidor/*" element={
          <ProtectedRoute roles={["CONSUMIDOR"]}>
            <ConsumidorDashboard />
          </ProtectedRoute>
        }>
          {/* podrás añadir sub-rutas aquí en el futuro */}
        </Route>

        {/* BASURERO */}
        <Route path="/basurero/*" element={
          <ProtectedRoute roles={["BASURERO"]}>
            <BasureroDashboard />
          </ProtectedRoute>
        }>
          <Route path="contenedores" element={<GestionContenedoresBasureros />} />
          <Route path="zonas"        element={<GestionZonasBasureros />}        />
        </Route>

        {/* Fallback */}
		<Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
