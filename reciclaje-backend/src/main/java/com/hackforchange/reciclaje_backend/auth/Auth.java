package com.hackforchange.reciclaje_backend.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.Gson;
import com.hackforchange.reciclaje_backend.security.JwtProvider;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Tuple;

public class Auth {

    private final MySQLPool client;
    private final Gson gson = new Gson();
    private final JwtProvider jwtProvider;

    public Auth(MySQLPool client, Vertx vertx) {
        this.client = client;
        try {
            this.jwtProvider = new JwtProvider(vertx);
        } catch (Exception e) {
            System.err.println("❌ Error al inicializar JwtProvider: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Router getRouter(Vertx vertx) {
        Router router = Router.router(vertx);

        // ⛑️ BodyHandler para que ctx.body() funcione
        router.route().handler(BodyHandler.create());

        // 🛡️ CORS en el subrouter
        router.route().handler(CorsHandler.create("https://www.ecobins.tech/*")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("Content-Type")
            .allowedHeader("Authorization")
            .allowedHeader("Accept")
            .allowCredentials(true)
        );

        System.out.println("🔗 Registrando rutas /register y /login...");
        router.post("/register").handler(this::handleRegister);
        router.post("/login").handler(this::handleLogin);

        return router;
    }

    private void handleRegister(RoutingContext ctx) {
        System.out.println("📩 Solicitud de registro recibida.");
        JsonObject body = ctx.body().asJsonObject();
        System.out.println("🧾 Cuerpo recibido como string: " + ctx.body().asString());

        String nombre = body.getString("nombre");
        String usuario = body.getString("usuario");
        String email = body.getString("email");
        String password = body.getString("password");
        String rol = body.getString("rol");

        if (nombre == null || usuario == null || email == null || password == null || rol == null) {
            System.out.println("⚠️ Campos obligatorios faltantes");
            ctx.response().setStatusCode(400).end("❌ Faltan campos obligatorios");
            return;
        }

        // Mover el hash de la contraseña a un bloque bloqueante
        ctx.vertx().executeBlocking(promise -> {
            String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            promise.complete(hashedPassword);
        }, res -> {
            if (res.succeeded()) {
                String hashedPassword = (String) res.result();
                String sql = "INSERT INTO usuario (nombre, usuario, email, password, rol) VALUES (?, ?, ?, ?, ?)";
                client.preparedQuery(sql).execute(Tuple.of(nombre, usuario, email, hashedPassword, rol), ar -> {
                    if (ar.succeeded()) {
                        System.out.println("✅ Usuario registrado exitosamente.");
                        ctx.response().setStatusCode(201).end("✅ Usuario registrado");
                    } else {
                        System.err.println("❌ Error al registrar usuario: " + ar.cause().getMessage());
                        ctx.response().setStatusCode(500).end("❌ Error al registrar: " + ar.cause().getMessage());
                    }
                });
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al procesar la contraseña");
            }
        });
    }

    private void handleLogin(RoutingContext ctx) {
        System.out.println("🔐 Solicitud de login recibida.");
        System.out.println("🧾 Cuerpo recibido como string: " + ctx.body().asString());

        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response()
               .setStatusCode(400)
               .putHeader("Content-Type", "application/json")
               .end(new JsonObject().put("error", "Cuerpo inválido o vacío").encode());
            return;
        }

        String email = body.getString("email") != null ? body.getString("email").trim() : null;
        String password = body.getString("password") != null ? body.getString("password").trim() : null;

        if (email == null || password == null) {
            ctx.response()
               .setStatusCode(400)
               .putHeader("Content-Type", "application/json")
               .end(new JsonObject().put("error", "Email y contraseña requeridos").encode());
            return;
        }

        String sql = "SELECT * FROM usuario WHERE email = ?";
        client.preparedQuery(sql).execute(Tuple.of(email), ar -> {
            if (ar.succeeded()) {
                int rowCount = ar.result().size();
                if (rowCount > 0) {
                    JsonObject user = ar.result().iterator().next().toJson();
                    String hashFromDb = user.getString("password");

                    // Ejecutar la verificación de la contraseña en un bloque bloqueante
                    ctx.vertx().executeBlocking(promise -> {
                        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashFromDb);
                        promise.complete(result.verified);
                    }, res -> {
                        if (res.succeeded() && (Boolean) res.result()) {
                            JsonObject tokenPayload = new JsonObject()
                                .put("id", user.getInteger("id"))
                                .put("email", user.getString("email"))
                                .put("rol", user.getString("rol"));
                            String token = jwtProvider.generateToken(tokenPayload);
                            JsonObject response = new JsonObject()
                                .put("token", token)
                                .put("user", tokenPayload);
                            ctx.response()
                               .putHeader("Content-Type", "application/json")
                               .end(response.encode());
                        } else {
                            ctx.response().setStatusCode(401).end("❌ Contraseña incorrecta");
                        }
                    });
                } else {
                    ctx.response().setStatusCode(404).end("❌ Usuario no encontrado");
                }
            } else {
                ctx.response().setStatusCode(500).end("❌ Error en la base de datos");
            }
        });
    }
}
