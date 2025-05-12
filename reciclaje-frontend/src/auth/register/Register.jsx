import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { User, Mail, Lock, BadgePlus } from "lucide-react";
import { motion } from "framer-motion";
import "../../static/css/auth/RegisterScreen.css";

export default function Register() {
  /* ───────── estados de pantalla ───────── */
  const [showWelcome, setShowWelcome]     = useState(false);
  const [showForm, setShowForm]           = useState(false);
  const [showExtraText, setShowExtraText] = useState(false);

  /* ───────── estados de formulario ─────── */
  const [form, setForm] = useState({
    nombre: "", usuario: "", email: "", password: "", rol: "CONSUMIDOR"
  });
  const [error, setError]     = useState("");
  const [isLoading, setLoading] = useState(false);

  const navigate = useNavigate();

  /* ───────── animaciones de entrada ────── */
  useEffect(() => {
    setShowWelcome(true);
    const t1 = setTimeout(() => setShowForm(true), 1000);
    const t2 = setTimeout(() => setShowExtraText(true), 6000);
    return () => { clearTimeout(t1); clearTimeout(t2); };
  }, []);

  /* ───────── helpers ───────────────────── */
  const handleChange = e =>
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));

  const isValidEmail = email =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  /* ───────── submit ────────────────────── */
  const handleSubmit = async e => {
    e.preventDefault();
    setError("");

    if (!isValidEmail(form.email)) {
      setError("❌ Por favor, introduce un email válido.");
      return;
    }

    setLoading(true);
    try {
      const res = await fetch("https://api.ecobins.tech/auth/register", {  // ⬅️ URL fija
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
        mode: "cors",
      });
      if (!res.ok) throw new Error(await res.text());

      alert("✅ Usuario registrado. Inicia sesión.");
      navigate("/");                              // ⬅️ SPA navigation
    } catch (err) {
      setError(err.message || "❌ Error al registrar");
    } finally {
      setLoading(false);
    }
  };

  /* ───────── render ────────────────────── */
  return (
    <div className="register-screen-container">
      {/* PANEL IZQUIERDO */}
      <div className="info-panel">
        <img src="/logo.png" alt="EcoTech" className="logo-main pulse-strong" />
        <img src="/logo.png" alt="logo-eco" className="bg-logo pulse-soft" />

        {showWelcome && (
          <motion.div className="info-content"
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ duration: 1 }}>
            <h1 className="fancy-heading">
              <span className="highlight">EcoTech</span> España
            </h1>
            <p>
              Únete a la plataforma de reciclaje inteligente.<br />
              Regístrate en segundos y accede a soluciones de<br />
              monitorización, rutas óptimas y métricas de impacto.
            </p>

            {showExtraText && (
              <motion.p className="extra-text fancy-text"
                         initial={{ opacity: 0, y: 10 }}
                         animate={{ opacity: 1, y: 0 }}
                         transition={{ duration: 1 }}>
                Empieza hoy a ahorrar costes y mejorar la
                sostenibilidad de tu municipio o empresa. ¡Te esperamos!
              </motion.p>
            )}
          </motion.div>
        )}
      </div>

      {/* PANEL DERECHO */}
      <div className="register-panel">
        {showForm && (
          <motion.div className="register-card"
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 1 }}>
            <h2>¡Crea tu cuenta!</h2>

            <form onSubmit={handleSubmit}>
              <div className="input-group">
                <User className="icon" />
                <input
                  name="nombre"
                  placeholder="Nombre"
                  value={form.nombre}
                  onChange={handleChange}
                  disabled={isLoading}
                  required
                />
              </div>

              <div className="input-group">
                <BadgePlus className="icon" />
                <input
                  name="usuario"
                  placeholder="Nombre de usuario"
                  value={form.usuario}
                  onChange={handleChange}
                  disabled={isLoading}
                  required
                />
              </div>

              <div className="input-group">
                <Mail className="icon" />
                <input
                  name="email"
                  type="email"
                  placeholder="Email"
                  value={form.email}
                  onChange={handleChange}
                  disabled={isLoading}
                  required
                />
              </div>

              <div className="input-group">
                <Lock className="icon" />
                <input
                  name="password"
                  type="password"
                  placeholder="Contraseña"
                  value={form.password}
                  onChange={handleChange}
                  disabled={isLoading}
                  required
                />
              </div>

              <div className="input-group">
                <select
                  name="rol"
                  value={form.rol}
                  onChange={handleChange}
                  disabled={isLoading}
                >
                  <option value="CONSUMIDOR">CONSUMIDOR</option>
                  <option value="BASURERO">BASURERO</option>
                  <option value="PROVEEDOR">PROVEEDOR</option>
                </select>
              </div>

              {error && <p className="error">{error}</p>}

              <button type="submit" disabled={isLoading}>
                {isLoading ? "Enviando…" : "Registrarse"}
              </button>
            </form>

            <div className="links">
              <Link to="/">¿Ya tienes cuenta? Inicia sesión</Link>
            </div>
          </motion.div>
        )}
      </div>
    </div>
  );
}
