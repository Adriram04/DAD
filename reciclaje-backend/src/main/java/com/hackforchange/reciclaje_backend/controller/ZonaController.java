package com.hackforchange.reciclaje_backend.controller;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class ZonaController {

    private final MySQLPool client;

    public ZonaController(MySQLPool client) {
        this.client = client;
    }

    public Router getRouter(Router router) {
        router.get("/zonas").handler(this::handleList);
        router.get("/zonas/with-contenedores").handler(this::handleZonasWithContenedorCount);
        router.post("/zonas").handler(this::handleCreate);
        router.put("/zonas/:id").handler(this::handleUpdate);
        router.delete("/zonas/:id").handler(this::handleDelete);
        return router;
    }

    private void handleZonasWithContenedorCount(RoutingContext ctx) {
        String queryZonas = "SELECT * FROM zona";

        client.query(queryZonas).execute(zonasResult -> {
            if (zonasResult.succeeded()) {
                RowSet<Row> zonasRows = zonasResult.result();
                JsonArray zonasArray = new JsonArray();

                for (Row zonaRow : zonasRows) {
                    JsonObject zona = new JsonObject()
                        .put("id", zonaRow.getInteger("id"))
                        .put("nombre", zonaRow.getString("nombre"))
                        .put("canal_mqtt", zonaRow.getString("canal_mqtt"));

                    zonasArray.add(zona);
                }

                // Ahora obtener los contenedores
                String contQuery = "SELECT c.id, c.nombre, c.id_zona, c.lleno, c.bloqueo " +
                                   "FROM contenedor c";

                client.query(contQuery).execute(contResult -> {
                    if (contResult.succeeded()) {
                        RowSet<Row> contRows = contResult.result();

                        for (Row contRow : contRows) {
                            int idZona = contRow.getInteger("id_zona");
                            JsonObject contenedor = new JsonObject()
                                .put("id", contRow.getInteger("id"))
                                .put("nombre", contRow.getString("nombre"))
                                .put("lleno", contRow.getBoolean("lleno"))
                                .put("bloqueo", contRow.getBoolean("bloqueo"));

                            // Buscar la zona correspondiente
                            for (int i = 0; i < zonasArray.size(); i++) {
                                JsonObject zona = zonasArray.getJsonObject(i);
                                if (zona.getInteger("id") == idZona) {
                                    if (!zona.containsKey("contenedores")) {
                                        zona.put("contenedores", new JsonArray());
                                    }
                                    zona.getJsonArray("contenedores").add(contenedor);
                                    break;
                                }
                            }
                        }

                        ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("zonas", zonasArray).encodePrettily());
                    } else {
                        ctx.response().setStatusCode(500).end("❌ Error al obtener contenedores");
                    }
                });

            } else {
                ctx.response().setStatusCode(500).end("❌ Error al obtener zonas");
            }
        });
    }


    private void handleList(RoutingContext ctx) {
        client.query("SELECT * FROM zona").execute(ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                JsonArray zonas = new JsonArray();
                for (Row row : rows) {
                    zonas.add(row.toJson());
                }
                ctx.response().putHeader("Content-Type", "application/json").end(
                    new JsonObject().put("zonas", zonas).encode()
                );
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al obtener zonas");
            }
        });
    }

    private void handleCreate(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String nombre = body.getString("nombre");
        String canal = body.getString("canal_mqtt");

        if (nombre == null || canal == null) {
            ctx.response().setStatusCode(400).end("❌ Faltan campos");
            return;
        }

        client.preparedQuery("INSERT INTO zona (nombre, canal_mqtt) VALUES (?, ?)")
            .execute(Tuple.of(nombre, canal), ar -> {
                if (ar.succeeded()) {
                    ctx.response().setStatusCode(201).end("✅ Zona creada");
                } else {
                    ctx.response().setStatusCode(500).end("❌ Error al crear zona");
                }
            });
    }

    private void handleUpdate(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject body = ctx.body().asJsonObject();
        String nombre = body.getString("nombre");
        String canal = body.getString("canal_mqtt");

        client.preparedQuery("UPDATE zona SET nombre = ?, canal_mqtt = ? WHERE id = ?")
            .execute(Tuple.of(nombre, canal, id), ar -> {
                if (ar.succeeded()) {
                    ctx.response().end("✅ Zona actualizada");
                } else {
                    ctx.response().setStatusCode(500).end("❌ Error al actualizar zona");
                }
            });
    }

    private void handleDelete(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        client.preparedQuery("DELETE FROM zona WHERE id = ?")
            .execute(Tuple.of(id), ar -> {
                if (ar.succeeded()) {
                    ctx.response().end("✅ Zona eliminada");
                } else {
                    ctx.response().setStatusCode(500).end("❌ Error al eliminar zona");
                }
            });
    }
}