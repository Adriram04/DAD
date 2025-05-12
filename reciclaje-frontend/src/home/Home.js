import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

export default function Home() {
  const navigate = useNavigate();
  const [redirected, setRedirected] = useState(false);
  const rol = localStorage.getItem("rol");
  console.log(rol);

  useEffect(() => {
	if (redirected) return;
	
    if (!rol) {
      navigate("/");
      return;
    }

    switch (rol) {
      case "ADMINISTRADOR":
        navigate("/admin");
        break;
      case "PROVEEDOR":
        navigate("/proveedor");
        break;
      case "CONSUMIDOR":
        navigate("/consumidor");
        break;
      case "BASURERO":
        navigate("/basurero");
        break;
      default:
        navigate("/");
        break;
    }
	
	setRedirected(true);
  }, [rol, navigate,redirected]);

  return null; // No renderiza nada, solo redirige
}
