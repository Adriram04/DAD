package com.hackforchange.reciclaje_backend.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.Gson;
import com.hackforchange.reciclaje_backend.security.JwtProvider;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Tuple;

public class Auth {

    private final MySQLPool client;
    private final Gson gson = new Gson();
    private final JwtProvider jwtProvider;

    public Auth(MySQLPool client, Vertx vertx) {
        this.client = client;
        this.jwtProvider = new JwtProvider(vertx);
    }

    /* ───────────────── Router ───────────────── */
    public Router getRouter(Vertx vertx) {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.route().handler(
            CorsHandler.create("https://www.ecobins.tech/*")
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

        router.post("/register").handler(this::handleRegister);
        router.post("/login").handler(this::handleLogin);

        return router;
    }

    /* ───────────────── Registro ───────────────── */
    private void handleRegister(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String nombre   = body.getString("nombre");
        String usuario  = body.getString("usuario");
        String email    = body.getString("email");
        String password = body.getString("password");
        String rol      = body.getString("rol");

        if (nombre == null || usuario == null || email == null || password == null || rol == null) {
            ctx.response().setStatusCode(400).end("❌ Faltan campos obligatorios");
            return;
        }

        ctx.vertx().executeBlocking(prom -> {
            prom.complete(
              BCrypt.withDefaults().hashToString(12, password.toCharArray()));
        }, res -> {
            String hash = (String) res.result();
            String sql  = "INSERT INTO usuario (nombre, usuario, email, password, rol) " +
                          "VALUES (?,?,?,?,?)";
            client.preparedQuery(sql)
                  .execute(Tuple.of(nombre, usuario, email, hash, rol), ar -> {
                      if (ar.succeeded())
                          ctx.response().setStatusCode(201).end("✅ Usuario registrado");
                      else
                          ctx.response().setStatusCode(500)
                             .end("❌ Error al registrar: " + ar.cause().getMessage());
                  });
        });
    }

    /* ───────────────── Login ───────────────── */
    private void handleLogin(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("❌ Cuerpo vació");
            return;
        }
        String email    = body.getString("email", "").trim();
        String password = body.getString("password", "").trim();
        if (email.isEmpty() || password.isEmpty()) {
            ctx.response().setStatusCode(400).end("❌ Email y contraseña requeridos");
            return;
        }

        /* 1) Traemos datos + puntos */
        String sql = "SELECT id, nombre, email, password, rol, puntos " +
                     "FROM usuario WHERE email = ? LIMIT 1";

        client.preparedQuery(sql).execute(Tuple.of(email), ar -> {
            if (ar.failed()) {
                ctx.response().setStatusCode(500).end("❌ Error en BD");
                return;
            }
            if (ar.result().size() == 0) {
                ctx.response().setStatusCode(404).end("❌ Usuario no encontrado");
                return;
            }

            JsonObject user = ar.result().iterator().next().toJson();
            String hashFromDb = user.getString("password");

            /* 2) Verificamos contraseña en hilo bloqueante */
            ctx.vertx().executeBlocking(prom -> {
                prom.complete(
                  BCrypt.verifyer().verify(password.toCharArray(), hashFromDb).verified);
            }, verif -> {
                if (!(Boolean) verif.result()) {
                    ctx.response().setStatusCode(401).end("❌ Contraseña incorrecta");
                    return;
                }

                /* 3) Generamos token y respondemos */
                JsonObject tokenPayload = new JsonObject()
                    .put("id",   user.getInteger("id"))
                    .put("email",user.getString("email"))
                    .put("rol",  user.getString("rol"));
                String token = jwtProvider.generateToken(tokenPayload);

                JsonObject userOut = new JsonObject()
                    .put("id",     user.getInteger("id"))
                    .put("nombre", user.getString("nombre"))
                    .put("email",  user.getString("email"))
                    .put("rol",    user.getString("rol"))
                    .put("puntos", user.getInteger("puntos"));

                ctx.response()
                   .putHeader("Content-Type","application/json")
                   .end(new JsonObject()
                        .put("token", token)
                        .put("user",  userOut)
                        .encode());
            });
        });
    }
}
