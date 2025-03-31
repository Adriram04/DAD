import { Outlet } from "react-router-dom";
import AdminNavbar from "./AdminNavbar";
import "../static/css/admin/adminLayout.css";

export default function AdminDashboard() {
  return (
    <div>
      <AdminNavbar />
      <main className="admin-main-content">
        <Outlet />
      </main>
    </div>
  );
}
