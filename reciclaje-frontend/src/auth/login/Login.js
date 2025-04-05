import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import "../../static/css/auth/AuthForm.css";

export default function Login() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false); // Control de carga
  const navigate = useNavigate();

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);
  
    try {
      const res = await fetch("https://api.ecobins.tech/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
        mode: "cors"
      });
  
      if (!res.ok) {
        // Intenta leer la respuesta en texto o en JSON
        let errorMsg;
        try {
          errorMsg = await res.text();
          console.error("Respuesta error (text):", errorMsg);
        } catch (err) {
          console.error("Error leyendo respuesta:", err);
          errorMsg = `HTTP error! status: ${res.status}`;
        }
        throw new Error(errorMsg || `HTTP error! status: ${res.status}`);
      }
  
      const data = await res.json();
      console.log("Respuesta exitosa:", data);
      localStorage.setItem("token", data.token);
      localStorage.setItem("rol", data.user.rol);
      navigate("/home");
    } catch (error) {
      console.error("Error en onSubmit, form:", form, error);
      setError("❌ Error de red o credenciales incorrectas");
    } finally {
      setIsLoading(false);
    }
  };
  

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h2>Iniciar sesión</h2>
      <input
        name="email"
        placeholder="Email"
        onChange={handleChange}
        required
      />
      <input
        name="password"
        type="password"
        placeholder="Contraseña"
        onChange={handleChange}
        required
      />
      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={isLoading}>
        {isLoading ? "Cargando..." : "Entrar"}
      </button>
      <p>
        ¿No tienes cuenta? <Link to="/register">Regístrate</Link>
      </p>
    </form>
  );
}
