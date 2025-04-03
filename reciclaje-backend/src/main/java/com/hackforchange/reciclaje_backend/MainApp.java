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

        // Obtener puerto desde variable de entorno (para Azure) o usar 8080 por defecto
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        System.out.println("🌐 Puerto HTTP configurado: " + port);

        Router router = Router.router(vertx);

        // CORS
        router.route().handler(CorsHandler.create("https://ecobins.tech")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("Content-Type")
            .allowedHeader("Authorization")
            .allowCredentials(true));
        
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
            .listen(port, "0.0.0.0", result -> {
                if (result.succeeded()) {
                    System.out.println("✅ Servidor HTTP iniciado en puerto " + port);
                    startPromise.complete();
                } else {
                    System.err.println("❌ Error al iniciar servidor: " + result.cause().getMessage());
                    result.cause().printStackTrace();
                    startPromise.fail(result.cause());
                }
            });
    }
}
