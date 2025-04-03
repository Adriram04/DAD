package com.hackforchange.reciclaje_backend;

import com.google.gson.Gson;
import com.hackforchange.reciclaje_backend.database.MySQLClientProvider;
import com.hackforchange.reciclaje_backend.auth.Auth;
import com.hackforchange.reciclaje_backend.config.DevDataLoader;
import com.hackforchange.reciclaje_backend.controller.ContenedorController;
import com.hackforchange.reciclaje_backend.controller.ProductosController;
import com.hackforchange.reciclaje_backend.controller.UserController;
import com.hackforchange.reciclaje_backend.controller.ZonaController;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.core.http.HttpMethod;

public class MainApp extends AbstractVerticle {

    private MySQLPool client;
    private final Gson gson = new Gson();

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("🚀 Iniciando MainApp...");

        JsonObject config = config();
        System.out.println("📦 Configuración cargada:");
        System.out.println(config.encodePrettily());

        // Crear conexión a MySQL
        System.out.println("🔌 Creando cliente MySQL...");
        client = MySQLClientProvider.createMySQLPool(vertx, config);
        System.out.println("✅ Cliente MySQL creado.");

        DevDataLoader.loadInitialUsers(client);

        System.out.println("🌐 Puerto HTTP configurado: ");

        Router router = Router.router(vertx);

        // CORS manual por si el CorsHandler no lo aplica correctamente
        router.route().handler(ctx -> {
            ctx.response()
                .putHeader("Access-Control-Allow-Origin", "https://ecobins.tech")
                .putHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Credentials", "true");
            ctx.next();
        });

        // CORS handler oficial de Vert.x (por si sí lo pilla)
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

        // Body handler
        router.route().handler(BodyHandler.create());

        // Subrouters
        Auth authRoutes = new Auth(client, vertx);
        router.mountSubRouter("/auth", authRoutes.getRouter(vertx));

        Router userRouter = Router.router(vertx);
        new UserController(client).getRouter(userRouter);
        router.mountSubRouter("/api", userRouter);

        Router zonaRouter = Router.router(vertx);
        new ZonaController(client).getRouter(zonaRouter);
        router.mountSubRouter("/api", zonaRouter);

        Router contenedorRouter = Router.router(vertx);
        new ContenedorController(client).getRouter(contenedorRouter);
        router.mountSubRouter("/api", contenedorRouter);

        Router productoRouter = Router.router(vertx);
        new ProductosController(client).getRouter(productoRouter);
        router.mountSubRouter("/api", productoRouter);

        System.out.println("🚀 Iniciando servidor HTTP...");
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(0, "0.0.0.0", result -> {
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
