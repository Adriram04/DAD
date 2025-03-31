public class MainApp extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("🚀 Iniciando MainApp...");
        
        // Configuración y conexión a MySQL
        JsonObject config = config();
        System.out.println("📦 Configuración cargada:");
        System.out.println(config.encodePrettily());
        
        client = MySQLClientProvider.createMySQLPool(vertx, config);
        System.out.println("✅ Cliente MySQL creado.");
        
        DevDataLoader.loadInitialUsers(client);

        int httpPort = config.getJsonObject("http").getInteger("port", 8080);
        System.out.println("🌐 Puerto HTTP configurado: " + httpPort);
        
        Router router = Router.router(vertx);

        // Configuración de CORS
        System.out.println("🛡️ Configurando CORS...");
        router.route().handler(CorsHandler.create("https://ecobins.tech")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.OPTIONS) // Asegúrate de permitir OPTIONS
            .allowedHeader("Content-Type")
            .allowedHeader("Authorization")
            .allowedHeader("Origin")
            .allowedHeader("Accept")
            .allowedHeader("X-Requested-With")
            .allowCredentials(true) // Si necesitas autenticación con cookies o headers
        );
        
        // Manejo explícito de OPTIONS (si sigue habiendo problemas con las preflight requests)
        router.options().handler(ctx -> {
            ctx.response()
                .setStatusCode(200)
                .putHeader("Access-Control-Allow-Origin", "https://ecobins.tech")  // Permite el acceso desde tu frontend
                .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Origin, Accept, X-Requested-With")
                .putHeader("Access-Control-Allow-Credentials", "true")
                .end();
        });

        // Configuración BodyHandler
        System.out.println("📦 Añadiendo BodyHandler...");
        router.route().handler(BodyHandler.create());
        
        // Rutas
        Auth authRoutes = new Auth(client, vertx);
        router.mountSubRouter("/auth", authRoutes.getRouter(vertx));
        // Otras rutas...
        
        // Iniciar servidor HTTP
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
