package com.hackforchange.reciclaje_backend;

import com.google.gson.Gson;
import com.hackforchange.reciclaje_backend.auth.Auth;
import com.hackforchange.reciclaje_backend.config.DevDataLoader;
import com.hackforchange.reciclaje_backend.controller.BasureroZonaController;
import com.hackforchange.reciclaje_backend.controller.ChatController;
import com.hackforchange.reciclaje_backend.controller.ContenedorController;
import com.hackforchange.reciclaje_backend.controller.GeoController;
import com.hackforchange.reciclaje_backend.controller.HealthController;
import com.hackforchange.reciclaje_backend.controller.MqttService;
import com.hackforchange.reciclaje_backend.controller.ProductosController;
import com.hackforchange.reciclaje_backend.controller.TarjetaController;
import com.hackforchange.reciclaje_backend.controller.UserController;
import com.hackforchange.reciclaje_backend.controller.UsuarioZonaController;
import com.hackforchange.reciclaje_backend.controller.WalletController;
import com.hackforchange.reciclaje_backend.controller.ZonaController;
import com.hackforchange.reciclaje_backend.database.MySQLClientProvider;
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
                .putHeader("Access-Control-Allow-Origin", "https://www.ecobins.tech")
                .putHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Credentials", "true");
            ctx.next();
        });

        // CORS oficial
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

        router.route().handler(BodyHandler.create());

        // Subrouters
        Auth authRoutes = new Auth(client, vertx);
        router.mountSubRouter("/auth", authRoutes.getRouter(vertx));

        // Google Wallet Service (opcional)
        GoogleWalletService walletService = null;
        try {
            walletService = new GoogleWalletService(
                System.getenv("GW_ISSUER"),
                System.getenv("GW_CLASS"),
                System.getenv("GW_KEY_JSON"));
            new WalletController(client, walletService).getRouter(router);
            new TarjetaController(client).getRouter(router);
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo inicializar GoogleWalletService: " + e.getMessage());
            System.err.println("   → Las rutas /wallet/*, /tarjetas quedarán inhabilitadas.");
        }

        // Rutas de asignación de zonas y basureros (si existieran)
        try {
            new UsuarioZonaController(client).getRouter(router);
            new BasureroZonaController(client).getRouter(router);
        } catch (Exception e) {
            System.err.println("⚠️ No se pudieron inicializar UsuarioZona o BasureroZona: " + e.getMessage());
            System.err.println("   → Las rutas relacionadas con asignación de zonas quedarán inhabilitadas.");
        }

        // Chat endpoint (opcional, solo si ENV está definido)
        String gcpCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (gcpCreds == null || gcpCreds.isEmpty()) {
            System.err.println("⚠️ GOOGLE_APPLICATION_CREDENTIALS no definido. /api/eco-chat deshabilitado.");
        } else {
            try {
                new ChatController(vertx).getRouter(router);
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo inicializar ChatController: " + e.getMessage());
                System.err.println("   → La ruta /api/eco-chat quedará inhabilitada.");
            }
        }

        // Rutas principales
        new UserController(client).getRouter(router);
        new ZonaController(client).getRouter(router);
        new ContenedorController(client).getRouter(router);
        new ProductosController(client).getRouter(router);
        new GeoController(client).getRouter(router);

        HealthController health = new HealthController(client);
        health.getRouter(router);

        System.out.println("🧪 Antes de crear servidor HTTP...");
        /* ─────────── MQTT SERVICE ─────────── */
        try {
            JsonObject mqttCfg = config.getJsonObject("mqtt");
            if (mqttCfg == null) {
                System.err.println("⚠️ Sección mqtt no encontrada → MQTT deshabilitado");
            } else {
                String mqttHost = mqttCfg.getString("host", "localhost");
                int    mqttPort = mqttCfg.getInteger("port", 1883);

                vertx.deployVerticle(
                    new MqttService(client, mqttHost, mqttPort),
                    new DeploymentOptions(),   // sin opciones extra
                    ar -> {
                        if (ar.succeeded())
                            System.out.println("🟢 MqttService desplegado (id " + ar.result() + ")");
                        else
                            System.err.println("❌ Error al desplegar MqttService: " + ar.cause());
                    });
            }
        } catch (Exception ex) {
            System.err.println("❌ Error preparando MqttService: " + ex.getMessage());
        }
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
