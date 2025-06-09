// AuthController.java
package com.hackforchange.reciclaje_backend.auth;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.core.http.HttpMethod;
import com.hackforchange.reciclaje_backend.repository.AuthRepository;
import com.hackforchange.reciclaje_backend.service.AuthService;
import io.vertx.mysqlclient.MySQLPool;

/**
 * Controller responsable de exponer los endpoints de autenticación:
 *  - POST /register
 *  - POST /login
 *
 * Aplica CORS para el dominio https://www.ecobins.tech y procesa
 * los cuerpos JSON de las peticiones.
 */
public class AuthController {

    /** Servicio que implementa la lógica de registro y login */
    private final AuthService auth;

    /**
     * Constructor que inyecta el repositorio de autenticación y el contexto Vert.x.
     *
     * @param client Pool de conexiones MySQL
     * @param vertx  Contexto de Vert.x para operaciones asíncronas
     */
    public AuthController(MySQLPool client, Vertx vertx) {
        this.auth = new AuthService(new AuthRepository(client), vertx);
    }

    /**
     * Configura y devuelve el router con las rutas de autenticación.
     *
     * @param vertx Instancia de Vert.x usada para crear el router
     * @return Router configurado con los handlers de registro y login
     */
    public Router getRouter(Vertx vertx) {
        Router router = Router.router(vertx);

        // BodyHandler para parsear JSON en el cuerpo de la petición
        router.route().handler(BodyHandler.create());

        // Configuración de CORS para permitir solo POST desde https://www.ecobins.tech
        router.route().handler(
            CorsHandler.create("https://www.ecobins.tech/*")
                .allowedMethod(HttpMethod.POST)
                .allowedHeader("Content-Type")
                .allowCredentials(true)
        );

        // Registro de rutas
        router.post("/register").handler(this::handleRegister);
        router.post("/login").handler(this::handleLogin);

        return router;
    }

    /**
     * Handler para POST /register.
     * Valida campos obligatorios y delega en el servicio de registro.
     *
     * @param ctx Contexto de la petición HTTP
     */
    private void handleRegister(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        String nombre   = body.getString("nombre");
        String usuario  = body.getString("usuario");
        String email    = body.getString("email");
        String password = body.getString("password");
        String rol      = body.getString("rol");

        // Validación de campos
        if (nombre == null || usuario == null || email == null ||
            password == null || rol == null) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ Faltan campos obligatorios");
            return;
        }

        // Llamada asíncrona al servicio de registro
        auth.register(nombre, usuario, email, password, rol)
            .onSuccess(v -> ctx.response()
                .setStatusCode(201)
                .end("✅ Usuario registrado"))
            .onFailure(err -> ctx.response()
                .setStatusCode(500)
                .end("❌ Error en registro: " + err.getMessage()));
    }

    /**
     * Handler para POST /login.
     * Valida credenciales, genera token en caso de éxito o devuelve error.
     *
     * @param ctx Contexto de la petición HTTP
     */
    private void handleLogin(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        String email    = body.getString("email", "").trim();
        String password = body.getString("password", "").trim();

        // Validación de credenciales no vacías
        if (email.isEmpty() || password.isEmpty()) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ Email y contraseña requeridos");
            return;
        }

        // Llamada asíncrona al servicio de login
        auth.login(email, password)
            .onSuccess(responseJson -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(responseJson.encode()))
            .onFailure(err -> {
                // Si el error indica credenciales incorrectas, devolvemos 401
                int status = err.getMessage().contains("no encontrado") ||
                             err.getMessage().contains("incorrecta")
                             ? 401 : 500;
                ctx.response()
                   .setStatusCode(status)
                   .end("❌ " + err.getMessage());
            });
    }
}
