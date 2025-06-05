package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class ContenedorController {

    private final MySQLPool client;

    public ContenedorController(MySQLPool client) {
        this.client = client;
    }

    public void getRouter(Router router) {
        router.get("/contenedores").handler(this::handleListContenedores);
        router.get("/contenedores/:id").handler(this::handleGetContenedor);
        router.post("/contenedores").handler(this::handleCreateContenedor);
        router.put("/contenedores/:id").handler(this::handleUpdateContenedor);
        router.delete("/contenedores/:id").handler(this::handleDeleteContenedor);
    }

    // GET /contenedores
    private void handleListContenedores(RoutingContext ctx) {
        String sql = ""
            + "SELECT c.id             AS contenedor_id, "
            + "       c.nombre         AS contenedor_nombre, "
            + "       c.capacidad_maxima, "
            + "       c.carga_actual, "
            + "       c.lat            AS lat, "
            + "       c.lon            AS lon, "
            + "       z.id             AS zona_id, "
            + "       z.nombre         AS zona_nombre "
            + "FROM contenedor c "
            + "JOIN zona z ON c.id_zona = z.id";

        client.query(sql).execute(ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                JsonArray arr = new JsonArray();
                for (Row row : rows) {
                    float capacidad = row.getFloat("capacidad_maxima");
                    float carga     = row.getFloat("carga_actual");
                    boolean lleno   = carga < capacidad && carga >= capacidad * 0.75;
                    boolean bloqueo = carga >= capacidad;

                    JsonObject c = new JsonObject()
                        .put("id",               row.getInteger("contenedor_id"))
                        .put("nombre",           row.getString ("contenedor_nombre"))
                        .put("capacidad_maxima", capacidad)
                        .put("carga_actual",     carga)
                        .put("lleno",            lleno)
                        .put("bloqueo",          bloqueo)
                        .put("lat",              row.getDouble("lat"))
                        .put("lon",              row.getDouble("lon"))
                        .put("zona", new JsonObject()
                            .put("id",   row.getInteger("zona_id"))
                            .put("nombre", row.getString("zona_nombre"))
                        );
                    arr.add(c);
                }
                ctx.response()
                   .putHeader("Content-Type","application/json")
                   .end(new JsonObject().put("contenedores", arr).encodePrettily());
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al obtener contenedores");
            }
        });
    }

    // GET /contenedores/:id
    private void handleGetContenedor(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ ID inválido");
            return;
        }

        String sql = ""
            + "SELECT c.id             AS contenedor_id, "
            + "       c.nombre         AS contenedor_nombre, "
            + "       c.capacidad_maxima, "
            + "       c.carga_actual, "
            + "       c.lat            AS lat, "
            + "       c.lon            AS lon, "
            + "       z.id             AS zona_id, "
            + "       z.nombre         AS zona_nombre "
            + "FROM contenedor c "
            + "JOIN zona z ON c.id_zona = z.id "
            + "WHERE c.id = ?";

        client.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rs = ar.result();
                if (!rs.iterator().hasNext()) {
                    ctx.response().setStatusCode(404).end("❌ Contenedor no encontrado");
                } else {
                    Row row = rs.iterator().next();
                    float capacidad = row.getFloat("capacidad_maxima");
                    float carga     = row.getFloat("carga_actual");
                    boolean lleno   = carga < capacidad && carga >= capacidad * 0.75;
                    boolean bloqueo = carga >= capacidad;

                    JsonObject c = new JsonObject()
                        .put("id",               row.getInteger("contenedor_id"))
                        .put("nombre",           row.getString ("contenedor_nombre"))
                        .put("capacidad_maxima", capacidad)
                        .put("carga_actual",     carga)
                        .put("lleno",            lleno)
                        .put("bloqueo",          bloqueo)
                        .put("lat",              row.getDouble("lat"))
                        .put("lon",              row.getDouble("lon"))
                        .put("zona", new JsonObject()
                            .put("id",   row.getInteger("zona_id"))
                            .put("nombre", row.getString("zona_nombre"))
                        );
                    ctx.response()
                       .putHeader("Content-Type","application/json")
                       .end(c.encodePrettily());
                }
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al obtener contenedor");
            }
        });
    }

    // POST /contenedores
    /** Espera JSON con { nombre, zonaId, lat, lon, capacidad_maxima, carga_actual } */
    private void handleCreateContenedor(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("❌ JSON requerido");
            return;
        }

        String nombre  = body.getString("nombre");
        Integer zonaId = body.getInteger("zonaId");
        Double lat     = body.getDouble("lat");
        Double lon     = body.getDouble("lon");
        Float capacidad= body.getFloat("capacidad_maxima", 100f);
        Float carga    = body.getFloat("carga_actual",      0f);
        Boolean lleno  = carga >= capacidad;

        if (nombre == null || zonaId == null || lat == null || lon == null) {
            ctx.response().setStatusCode(400).end("❌ Faltan campos obligatorios");
            return;
        }

        String sql = ""
            + "INSERT INTO contenedor "
            + "(nombre, id_zona, lat, lon, capacidad_maxima, carga_actual, lleno) "
            + "VALUES (?,?,?,?,?,?,?)";

        client.preparedQuery(sql).execute(
            Tuple.of(nombre, zonaId, lat, lon, capacidad, carga, lleno),
            ar -> {
                if (ar.succeeded()) {
                    ctx.response().setStatusCode(201).end("✅ Contenedor creado");
                } else {
                    ctx.response().setStatusCode(500)
                       .end("❌ Error al crear contenedor: " + ar.cause().getMessage());
                }
            });
    }

    // PUT /contenedores/:id
    private void handleUpdateContenedor(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ ID inválido");
            return;
        }

        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("❌ JSON requerido");
            return;
        }

        String  nombre   = body.getString("nombre");
        Integer zonaId   = body.getInteger("zonaId");
        Double  lat      = body.getDouble("lat");
        Double  lon      = body.getDouble("lon");
        Float   capacidad= body.getFloat("capacidad_maxima");
        
        if (nombre == null || zonaId == null || lat == null || lon == null || capacidad == null) {
            ctx.response().setStatusCode(400).end("❌ Faltan campos obligatorios");
            return;
        }

        String sql = ""
            + "UPDATE contenedor SET "
            + " nombre = ?, id_zona = ?, lat = ?, lon = ?, capacidad_maxima = ? "
            + "WHERE id = ?";

        client.preparedQuery(sql).execute(
            Tuple.of(nombre, zonaId, lat, lon, capacidad, id),
            ar -> {
                if (ar.succeeded()) {
                    if (ar.result().rowCount() == 0) {
                        ctx.response().setStatusCode(404).end("❌ Contenedor no encontrado");
                    } else {
                        ctx.response().end("✅ Contenedor actualizado");
                    }
                } else {
                    ctx.response().setStatusCode(500).end("❌ Error al actualizar contenedor");
                }
            });
    }

    // DELETE /contenedores/:id
    private void handleDeleteContenedor(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ ID inválido");
            return;
        }

        client.preparedQuery("DELETE FROM contenedor WHERE id = ?")
              .execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                if (ar.result().rowCount() == 0) {
                    ctx.response().setStatusCode(404).end("❌ Contenedor no encontrado");
                } else {
                    ctx.response().end("✅ Contenedor eliminado");
                }
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al eliminar contenedor");
            }
        });
    }
}
