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

    // Obtener el puerto de la variable de entorno de Azure
    int httpPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    System.out.println("🌐 Puerto HTTP configurado: " + httpPort);

    Router router = Router.router(vertx);

    // Forzar HTTPS
    router.route().handler(ctx -> {
        String proto = ctx.request().getHeader("x-forwarded-proto");
        if ("http".equalsIgnoreCase(proto)) {
            String host = ctx.request().host();
            String uri = ctx.request().uri();
            ctx.response()
                .setStatusCode(301)
                .putHeader("Location", "https://" + host + uri)
                .end();
        } else {
            ctx.next();
        }
    });

    System.out.println("🛡️ Configurando CORS...");
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

    // Registrar rutas
    System.out.println("🔐 Registrando rutas...");
    Auth authRoutes = new Auth(client, vertx);
    router.mountSubRouter("/auth", authRoutes.getRouter(vertx));

    UserController userController = new UserController(client);
    Router userRouter = Router.router(vertx);
    userController.getRouter(userRouter);
    router.mountSubRouter("/api", userRouter);

    ZonaController zonaController = new ZonaController(client);
    Router zonaRouter = Router.router(vertx);
    zonaController.getRouter(zonaRouter);
    router.mountSubRouter("/api", zonaRouter);

    ContenedorController contenedorController = new ContenedorController(client);
    Router contenedorRouter = Router.router(vertx);
    contenedorController.getRouter(contenedorRouter);
    router.mountSubRouter("/api", contenedorRouter);

    ProductosController productoController = new ProductosController(client);
    Router productoRouter = Router.router(vertx);
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
