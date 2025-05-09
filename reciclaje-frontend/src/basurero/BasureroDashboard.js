import { Outlet } from "react-router-dom";
import BasureroNavbar from "./BasureroNavbar";
import "../static/css/basurero/basureroLayout.css";

export default function BasureroDashboard() {
  return (
    <div>
      <BasureroNavbar />
      <main className="basurero-main-content">
        <Outlet />
      </main>
    </div>
  );
}