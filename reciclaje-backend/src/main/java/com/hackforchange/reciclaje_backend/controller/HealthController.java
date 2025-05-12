package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class HealthController {

    private final MySQLPool client;

    public HealthController(MySQLPool client) {
        this.client = client;
    }

    /**
     * Monta dos rutas en el router:
     *  - GET /health         → devuelve “Backend is up!”
     *  - GET /health/:uid    → consulta en tabla tarjeta por ese uid y retorna datos de usuario
     */
    public void getRouter(Router router) {
        // Ruta de comprobación básica
        router.get("/health").handler(this::handleHealth);

        // Nueva ruta para validar UID de tarjeta y obtener datos de usuario
        router.get("/health/:uid").handler(this::handleCheckUid);
    }

    private void handleHealth(RoutingContext ctx) {
        ctx.response()
           .setStatusCode(200)
           .putHeader("Content-Type", "text/plain")
           .end("Backend is up!");
    }

    private void handleCheckUid(RoutingContext ctx) {
        String uid = ctx.pathParam("uid");
        if (uid == null || uid.isBlank()) {
            ctx.response()
               .setStatusCode(400)
               .putHeader("Content-Type", "application/json")
               .end(new JsonObject()
                   .put("error", "Missing UID")
                   .encode());
            return;
        }

        // Ahora hacemos JOIN entre tarjeta y usuario para traer los datos
        String sql = "SELECT u.id, u.nombre, u.usuario AS username, u.email, u.rol "
                   + "FROM tarjeta t "
                   + "JOIN usuario u ON t.id_consumidor = u.id "
                   + "WHERE t.uid = ?";
        client.preparedQuery(sql)
              .execute(Tuple.of(uid), ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                if (!rows.iterator().hasNext()) {
                    // No existe tarjeta → no autorizado
                    JsonObject resp = new JsonObject()
                        .put("authorized", false);
                    ctx.response()
                       .setStatusCode(200)
                       .putHeader("Content-Type", "application/json")
                       .end(resp.encode());
                } else {
                    // Encontramos al usuario
                    Row row = rows.iterator().next();
                    JsonObject user = new JsonObject()
                        .put("id", row.getInteger("id"))
                        .put("nombre", row.getString("nombre"))
                        .put("usuario", row.getString("username"))
                        .put("email", row.getString("email"))
                        .put("rol", row.getString("rol"));

                    JsonObject resp = new JsonObject()
                        .put("authorized", true)
                        .put("user", user);

                    ctx.response()
                       .setStatusCode(200)
                       .putHeader("Content-Type", "application/json")
                       .end(resp.encode());
                }
            } else {
                ctx.response()
                   .setStatusCode(500)
                   .putHeader("Content-Type", "application/json")
                   .end(new JsonObject()
                       .put("error", "DB error")
                       .put("details", ar.cause().getMessage())
                       .encode());
            }
        });
    }
}
