package com.hackforchange.reciclaje_backend;

import com.google.gson.Gson;
import com.hackforchange.reciclaje_backend.auth.AuthController;
import com.hackforchange.reciclaje_backend.config.DevDataLoader;
import com.hackforchange.reciclaje_backend.controller.BasureroZonaController;
import com.hackforchange.reciclaje_backend.controller.ChatController;
import com.hackforchange.reciclaje_backend.controller.ContenedorController;
import com.hackforchange.reciclaje_backend.controller.GeoController;
import com.hackforchange.reciclaje_backend.controller.HealthController;
import com.hackforchange.reciclaje_backend.controller.MqttVerticle;
import com.hackforchange.reciclaje_backend.controller.ProductosController;
import com.hackforchange.reciclaje_backend.controller.TarjetaController;
import com.hackforchange.reciclaje_backend.controller.UserController;
import com.hackforchange.reciclaje_backend.controller.UsuarioZonaController;
import com.hackforchange.reciclaje_backend.controller.WalletController;
import com.hackforchange.reciclaje_backend.controller.ZonaController;
import com.hackforchange.reciclaje_backend.database.MySQLClientProvider;
import com.hackforchange.reciclaje_backend.repository.AuthRepository;
import com.hackforchange.reciclaje_backend.service.AuthService;
import com.hackforchange.reciclaje_backend.wallet.GoogleWalletService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.mysqlclient.MySQLPool;

public class MainApp extends AbstractVerticle {

    private MySQLPool client;
    private final Gson gson = new Gson();

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject config = config();
        System.out.println("📦 Config loaded:\n" + config.encodePrettily());

        // Initialize MySQL client
        client = MySQLClientProvider.createMySQLPool(vertx, config);
        DevDataLoader.loadInitialUsers(client);

        // Create router and middleware
        Router router = Router.router(vertx);
        setupCors(router);
        router.route().handler(BodyHandler.create());

        // ─── Auth ───
        AuthController authController = new AuthController(client, vertx);
        router.mountSubRouter("/auth", authController.getRouter(vertx));

        // ─── Controllers ───
        
        new ContenedorController(client).getRouter(router);
        new ZonaController(client).getRouter(router);
        new UsuarioZonaController(client).getRouter(router);
        new TarjetaController(client).getRouter(router);
        new ProductosController(client).getRouter(router);
        new BasureroZonaController(client).getRouter(router);
        new UserController(client).getRouter(router);
        new GeoController(client).getRouter(router);
        new HealthController(client).getRouter(router);

        // ─── Wallet ───
        try {
            var walletSvc = new GoogleWalletService(
                System.getenv("GW_ISSUER"),
                System.getenv("GW_CLASS"),
                System.getenv("GW_KEY_JSON")
            );
            new WalletController(client, walletSvc).getRouter(router);
        } catch (Exception e) {
            System.err.println("⚠️ Wallet disabled: " + e.getMessage());
        }

        // ─── Chat (optional) ───
        String gcpCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (gcpCreds != null && !gcpCreds.isBlank()) {
            new ChatController(vertx).getRouter(router);
        }

        // ─── MQTT Verticle ───
        vertx.deployVerticle(
            new MqttVerticle(),
            new DeploymentOptions().setConfig(config),
            ar -> {
                if (ar.succeeded()) System.out.println("🟢 MQTT deployed");
                else                System.err.println("❌ MQTT failed: " + ar.cause());
            }
        );

        // ─── Start HTTP server ───
        int port = getPort();
        vertx.createHttpServer()
             .requestHandler(router)
             .listen(port, "0.0.0.0", ar -> {
                if (ar.succeeded()) {
                    System.out.println("✅ HTTP listening on port " + port);
                    startPromise.complete();
                } else {
                    startPromise.fail(ar.cause());
                }
             });
    }

    private int getPort() {
        var p = System.getenv("PORT");
        try { return p != null ? Integer.parseInt(p) : 8080; }
        catch (NumberFormatException e) { return 8080; }
    }

    private void setupCors(Router router) {
        router.route().handler(ctx -> {
            ctx.response()
               .putHeader("Access-Control-Allow-Origin", "https://www.ecobins.tech")
               .putHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
               .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
               .putHeader("Access-Control-Allow-Credentials", "true");
            ctx.next();
        });
        router.route().handler(CorsHandler.create("https://www.ecobins.tech")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("Content-Type")
            .allowedHeader("Authorization")
            .allowCredentials(true)
        );
    }
}
