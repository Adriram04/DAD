package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class GeoController {

    private final MySQLPool client;
    public GeoController(MySQLPool client) { this.client = client; }

    /* ─────────────────────────────────────────────── */
    public void getRouter(Router router) {
        router.get   ("/zonas/geo") .handler(this::handleListZonasGeo);
        router.post  ("/zonas")     .handler(this::handleCreateZona);
        router.put   ("/zonas/:id") .handler(this::handleUpdateZona);
        router.delete("/zonas/:id") .handler(this::handleDeleteZona);
    }

    /* ========= GET /zonas/geo ========= */
    private void handleListZonasGeo(RoutingContext ctx) {
        final String sql = "SELECT id,nombre,geom FROM zona";

        client.query(sql).execute(ar -> {
            if (ar.succeeded()) {
                JsonArray zonas = new JsonArray();

                for (Row row : ar.result()) {
                    // 1️⃣  Recuperamos geom de forma segura
                    Object rawGeom = row.getValue("geom");
                    JsonArray geom = null;

                    if (rawGeom != null) {
                        if (rawGeom instanceof JsonArray) {
                            geom = (JsonArray) rawGeom;
                        } else {               // suele llegar como String
                            geom = new JsonArray(rawGeom.toString());
                        }
                    }

                    zonas.add(new JsonObject()
                        .put("id",     row.getInteger("id"))
                        .put("nombre", row.getString("nombre"))
                        .put("geom",   geom)        // array de pares [lat,lon]
                    );
                }

                ctx.response().putHeader("Content-Type","application/json")
                               .end(new JsonObject().put("zonas", zonas).encode());

            } else {
                ctx.response().setStatusCode(500).end("❌ Error al obtener zonas");
            }
        });
    }

    /* ========= POST /zonas ========= */
    private void handleCreateZona(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) { ctx.response().setStatusCode(400).end("❌ JSON requerido"); return; }

        String     nombre = body.getString("nombre");
        JsonArray  geom   = body.getJsonArray("geom");   // 2️⃣  ahora JsonArray
        if (nombre == null || geom == null) {
            ctx.response().setStatusCode(400).end("❌ nombre y geom son obligatorios");
            return;
        }

        String canal = "zona/" + nombre.toLowerCase().replace(" ","_");
        String sql   = "INSERT INTO zona (nombre, canal_mqtt, geom) VALUES (?,?,?)";

        client.preparedQuery(sql).execute(Tuple.of(nombre, canal, geom), ar -> {
            if (ar.succeeded())
                ctx.response().setStatusCode(201).end("✅ Zona creada");
            else
                ctx.response().setStatusCode(500).end("❌ Error al crear zona");
        });
    }

    /* ========= PUT /zonas/:id ========= */
    private void handleUpdateZona(RoutingContext ctx) {
        Integer id;
        try { id = Integer.parseInt(ctx.pathParam("id")); }
        catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ ID inválido"); return;
        }

        JsonObject body = ctx.body().asJsonObject();
        if (body == null) { ctx.response().setStatusCode(400).end("❌ JSON requerido"); return; }

        String    nombre = body.getString("nombre");
        JsonArray geom   = body.getJsonArray("geom");  // 2️⃣

        if (nombre == null || geom == null) {
            ctx.response().setStatusCode(400).end("❌ nombre y geom son obligatorios");
            return;
        }

        String sql = "UPDATE zona SET nombre = ?, geom = ? WHERE id = ?";

        client.preparedQuery(sql).execute(Tuple.of(nombre, geom, id), ar -> {
            if (ar.succeeded()) {
                if (ar.result().rowCount() == 0)
                    ctx.response().setStatusCode(404).end("❌ Zona no encontrada");
                else
                    ctx.response().end("✅ Zona actualizada");
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al actualizar zona");
            }
        });
    }

    /* ========= DELETE /zonas/:id ========= */
    private void handleDeleteZona(RoutingContext ctx) {
        Integer id;
        try { id = Integer.parseInt(ctx.pathParam("id")); }
        catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ ID inválido"); return;
        }

        String sql = "DELETE FROM zona WHERE id = ?";

        client.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                if (ar.result().rowCount() == 0)
                    ctx.response().setStatusCode(404).end("❌ Zona no encontrada");
                else
                    ctx.response().end("✅ Zona eliminada");
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al eliminar zona");
            }
        });
    }
}
