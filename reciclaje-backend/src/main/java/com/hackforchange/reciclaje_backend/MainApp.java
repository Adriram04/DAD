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

        int httpPort = config.getJsonObject("http").getInteger("port", 8080);
        System.out.println("🌐 Puerto HTTP configurado: " + httpPort);

        Router router = Router.router(vertx);

        System.out.println("🛡️ Configurando CORS...");
        router.route().handler(CorsHandler.create("https://ecobins.tech")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.OPTIONS) // Añadir OPTIONS si es necesario
            .allowedHeader("Content-Type")
            .allowedHeader("Authorization")
            .allowedHeader("Access-Control-Allow-Origin")
            .allowedHeader("Access-Control-Allow-Methods")
            .allowedHeader("Access-Control-Allow-Credentials")
            .allowedHeader("Access-Control-Allow-Headers")); // Añadir todos los encabezados necesarios

        System.out.println("📦 Añadiendo BodyHandler...");
        router.route().handler(BodyHandler.create());

        System.out.println("🔐 Registrando rutas de autenticación...");
        Auth authRoutes = new Auth(client, vertx);
        router.mountSubRouter("/auth", authRoutes.getRouter(vertx));
        System.out.println("👥 Registrando rutas de usuario...");
        Router userRouter = Router.router(vertx);
        UserController userController = new UserController(client);
        userController.getRouter(userRouter);
        router.mountSubRouter("/api", userRouter);
        System.out.println("👥 Registrando rutas de zonas...");
        Router zonaRouter = Router.router(vertx);
        ZonaController zonaController = new ZonaController(client);
        zonaController.getRouter(zonaRouter);
        router.mountSubRouter("/api", zonaRouter);
        System.out.println("👥 Registrando rutas de contenedores...");
        Router contenedorRouter = Router.router(vertx);
        ContenedorController contenedorController = new ContenedorController(client);
        contenedorController.getRouter(contenedorRouter);
        router.mountSubRouter("/api", contenedorRouter);
        System.out.println("👥 Registrando rutas de productos...");
        Router productoRouter = Router.router(vertx);
        ProductosController productoController = new ProductosController(client);
        productoController.getRouter(productoRouter);
        router.mountSubRouter("/api", productoRouter);

        System.out.println("🚀 Iniciando servidor HTTP...");
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(httpPort, result -> {
                if (result.succeeded()) {
                    System.out.println("✅ Servidor HTTP en puerto " + httpPort);
                    startPromise.complete();
                } else {
                    System.err.println("❌ Error al iniciar servidor: " + result.cause().getMessage());
                    result.cause().printStackTrace();
                    startPromise.fail(result.cause());
                }
            });
    }
}
