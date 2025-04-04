package com.hackforchange.reciclaje_backend;

import com.google.gson.Gson;
import com.hackforchange.reciclaje_backend.auth.Auth;
import com.hackforchange.reciclaje_backend.config.DevDataLoader;
import com.hackforchange.reciclaje_backend.controller.ContenedorController;
import com.hackforchange.reciclaje_backend.controller.ProductosController;
import com.hackforchange.reciclaje_backend.controller.UserController;
import com.hackforchange.reciclaje_backend.controller.ZonaController;
import com.hackforchange.reciclaje_backend.database.MySQLClientProvider;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.core.http.HttpMethod;
import io.vertx.mysqlclient.MySQLPool;

public class MainApp extends AbstractVerticle {

    private MySQLPool client;
    private final Gson gson = new Gson();

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("🚀 Iniciando MainApp...");

        JsonObject config = config();
        System.out.println("📦 Configuración cargada:");
        System.out.println(config.encodePrettily());

        // Verificar puerto proporcionado por Azure
        String portEnv = System.getenv("PORT");
        int port;
        if (portEnv == null) {
            System.err.println("⚠️ Variable de entorno PORT no definida. Usando puerto 8080 por defecto.");
            port = 8080;
        } else {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("❌ Valor inválido para PORT: " + portEnv + ". Usando 8080.");
                port = 8080;
            }
        }
        System.out.println("🌐 Puerto HTTP: " + port);

        // Crear cliente MySQL
        try {
            System.out.println("🔌 Creando cliente MySQL...");
            client = MySQLClientProvider.createMySQLPool(vertx, config);
            System.out.println("✅ Cliente MySQL creado.");
        } catch (Exception e) {
            System.err.println("❌ Error al crear cliente MySQL: " + e.getMessage());
            startPromise.fail(e);
            return;
        }

        DevDataLoader.loadInitialUsers(client);

        // Configurar router
        Router router = Router.router(vertx);

        // CORS manual
        router.route().handler(ctx -> {
            ctx.response()
                .putHeader("Access-Control-Allow-Origin", "https://ecobins.tech")
                .putHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Credentials", "true");
            ctx.next();
        });

        // CORS oficial
        router.route().handler(CorsHandler.create("https://ecobins.tech")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("Content-Type")
            .allowedHeader("Authorization")
            .allowCredentials(true)
        );

        router.route().handler(BodyHandler.create());

        // Subrouters
        Auth authRoutes = new Auth(client, vertx);
        router.mountSubRouter("/auth", authRoutes.getRouter(vertx));

        new UserController(client).getRouter(router);
        new ZonaController(client).getRouter(router);
        new ContenedorController(client).getRouter(router);
        new ProductosController(client).getRouter(router);

        System.out.println("🧪 Antes de crear servidor HTTP...");

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port, "0.0.0.0", result -> {
                if (result.succeeded()) {
                    System.out.println("✅ Servidor HTTP iniciado en puerto " + result.result().actualPort());
                    startPromise.complete();
                } else {
                    System.err.println("❌ Error al iniciar servidor: " + result.cause().getMessage());
                    result.cause().printStackTrace();
                    startPromise.fail(result.cause());
                }
            });
    }
}
