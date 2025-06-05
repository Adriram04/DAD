// src/admin/AdminProfile.jsx
import "../static/css/admin/adminProfile.css";

export default function AdminProfile() {
  /* valores que ya guardas al iniciar sesión */
  const email = localStorage.getItem("email") ?? "admin@hackforchange.com";
  const rol   = localStorage.getItem("rol")   ?? "ADMINISTRADOR";

  /* rutas opcionales si añades imágenes a /public */
  const banner = "/banner-admin.jpg";
  const avatar = "/avatar-placeholder.png";

  return (
    <section className="profile-card">
      {/* ───── Banner ───── */}
      <div
        className="banner"
        style={{ backgroundImage: `url(${banner})` }}
      />

      {/* ───── Avatar ───── */}
      <div className="avatar-wrapper">
        <img
          className="avatar"
          src={avatar}
          alt="avatar admin"
          onError={(e) => {
            /* Fallback SVG sin icono roto */
            e.currentTarget.onerror = null;
            e.currentTarget.src =
              "data:image/svg+xml;utf8," +
              encodeURIComponent(`
                <svg xmlns='http://www.w3.org/2000/svg' width='96' height='96' viewBox='0 0 24 24' fill='#9ca3af'>
                  <circle cx='12' cy='8' r='4'/>
                  <path d='M4 20c0-4 4-6 8-6s8 2 8 6'/>
                </svg>
              `);
          }}
        />
      </div>

      {/* ───── Contenido ───── */}
      <div className="info">
        <h2 className="name">Administrador</h2>
        <span className="badge">{rol}</span>

        <p className="email">{email}</p>

        <p className="hint">
          Desde aquí puedes navegar por las secciones con el menú superior.
        </p>
      </div>
    </section>
  );
}
