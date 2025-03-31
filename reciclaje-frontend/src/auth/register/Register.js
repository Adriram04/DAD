import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import "../../static/css/auth/AuthForm.css";

export default function Register() {
  const [form, setForm] = useState({
    nombre: "",
    email: "",
    password: "",
    rol: "CONSUMIDOR",
  });
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const isValidEmail = (email) =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!isValidEmail(form.email)) {
      setError("❌ Por favor, introduce un email válido.");
      return;
    }

    const res = await fetch("https://api.ecobins.tech/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(form),
    });

    if (res.ok) {
      alert("✅ Usuario registrado. Inicia sesión.");
      navigate("/");
    } else {
      setError("❌ Error al registrar");
    }
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h2>Registro</h2>
      <input
        name="nombre"
        placeholder="Nombre"
        onChange={handleChange}
        required
      />
      <input
        name="email"
        type="email"
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
      <select name="rol" onChange={handleChange} value={form.rol}>
        <option value="CONSUMIDOR">CONSUMIDOR</option>
        <option value="BASURERO">BASURERO</option>
        <option value="PROVEEDOR">PROVEEDOR</option>
      </select>
      {error && <p className="error">{error}</p>}
      <button type="submit">Registrarse</button>
      <p>
        ¿Ya tienes cuenta?{" "}
        <Link to="/" className="back-link">Inicia sesión</Link>
      </p>
    </form>
  );
}
