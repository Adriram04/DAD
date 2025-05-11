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
     *  - GET /health/:uid    → consulta en tabla tarjeta por ese uid
     */
    public void getRouter(Router router) {
        // Ruta de comprobación básica
        router.get("/health").handler(this::handleHealth);

        // Nueva ruta para validar UID de tarjeta
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
               .end(new JsonObject().put("error", "Missing UID").encode());
            return;
        }

        // Consulta: ¿existe al menos un registro en tarjeta con este UID?
        String sql = "SELECT COUNT(*) AS cnt FROM tarjeta WHERE uid = ?";
        client.preparedQuery(sql)
              .execute(Tuple.of(uid), ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                Row row = rows.iterator().next();
                boolean exists = row.getInteger("cnt") > 0;

                JsonObject resp = new JsonObject()
                    .put("authorized", exists);
                ctx.response()
                   .setStatusCode(200)
                   .putHeader("Content-Type", "application/json")
                   .end(resp.encode());
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
