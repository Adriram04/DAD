import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";   // ⬅️ usamos useNavigate
import { Mail, Lock } from "lucide-react";
import { motion } from "framer-motion";
import "../../static/css/auth/LoginScreen.css";

export default function Login() {
  /* ───────── estados de pantalla ───────── */
  const [showWelcome, setShowWelcome]     = useState(false);
  const [showForm, setShowForm]           = useState(false);
  const [showExtraText, setShowExtraText] = useState(false);

  /* ───────── estados de formulario ─────── */
  const [form, setForm]   = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [isLoading, setLoading] = useState(false);

  const navigate = useNavigate();                    // ⬅️ hook de React Router

  /* ───────── animaciones de entrada ────── */
  useEffect(() => {
    setShowWelcome(true);
    const t1 = setTimeout(() => setShowForm(true), 1000);
    const t2 = setTimeout(() => setShowExtraText(true), 6000);
    return () => { clearTimeout(t1); clearTimeout(t2); };
  }, []);

  /* ───────── handlers ──────────────────── */
  const handleChange = e =>
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async e => {
    e.preventDefault();
    setError(""); setLoading(true);

    try {
      const res = await fetch("https://api.ecobins.tech/auth/login", { // ⬅️ URL fija
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
        mode: "cors",
      });
      if (!res.ok) throw new Error(await res.text());

      const { token, user: { rol } } = await res.json();
      localStorage.setItem("token", token);
      localStorage.setItem("rol",   rol);

      navigate("/home");   // ⬅️ cambio de ruta SIN recargar la página
    } catch {
      setError("❌ Credenciales incorrectas o error de red");
    } finally {
      setLoading(false);
    }
  };

  /* ───────── render ────────────────────── */
  return (
    <div className="login-screen-container">
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
              Somos una empresa de reciclaje inteligente. <br/>
              Optimiza la recogida, monitoriza los contenedores
              en tiempo real y reduce costes con nuestra plataforma.
            </p>

            {showExtraText && (
              <motion.p className="extra-text fancy-text"
                         initial={{ opacity: 0, y: 10 }}
                         animate={{ opacity: 1, y: 0 }}
                         transition={{ duration: 1 }}>
                Descubre nuestras nuevas métricas en tiempo real,
                informes personalizados y ¡mucho más! Únete ya y ahorra
                en cada ruta de recogida.
              </motion.p>
            )}
          </motion.div>
        )}
      </div>

      {/* PANEL DERECHO */}
      <div className="login-panel">
        {showForm && (
          <motion.div className="login-card"
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 1 }}>
            <h2>¡Inicia tu sesión!</h2>

            <form onSubmit={handleSubmit}>
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

              {error && <p className="error">{error}</p>}

              <button type="submit" disabled={isLoading}>
                {isLoading ? "Cargando…" : "Entrar"}
              </button>
            </form>

            <div className="links">
              <Link to="/register">Regístrate</Link>
              <Link to="/forgot-password">¿Olvidaste tu contraseña?</Link>
            </div>
          </motion.div>
        )}
      </div>
    </div>
  );
}
