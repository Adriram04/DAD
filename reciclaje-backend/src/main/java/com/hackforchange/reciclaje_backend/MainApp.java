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

        System.out.println("🛡 Configurando CORS...");
        // Habilitar CORS solo para el dominio de producción en Azure
        router.route().handler(CorsHandler.create("https://ecobins.tech")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("Content-Type")
            .allowedHeader("Authorization")
            .allowCredentials(true));

        System.out.println("📦 Añadiendo BodyHandler...");
        router.route().handler(BodyHandler.create());

        // Registro de subrouters
        router.mountSubRouter("/auth", new Auth(client, vertx).getRouter(vertx));
        router.mountSubRouter("/api", new UserController(client).getRouter(Router.router(vertx)));
        router.mountSubRouter("/api", new ZonaController(client).getRouter(Router.router(vertx)));
        router.mountSubRouter("/api", new ContenedorController(client).getRouter(Router.router(vertx)));
        router.mountSubRouter("/api", new ProductosController(client).getRouter(Router.router(vertx)));

        System.out.println("🚀 Iniciando servidor HTTP...");
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888,"0.0.0.0", result -> {
                if (result.succeeded()) {
                    System.out.println("✅ Servidor HTTP en puerto ");
                    startPromise.complete();
                } else {
                    System.err.println("❌ Error al iniciar servidor: " + result.cause().getMessage());
                    result.cause().printStackTrace();
                    startPromise.fail(result.cause());
                }
            });
    }
}
