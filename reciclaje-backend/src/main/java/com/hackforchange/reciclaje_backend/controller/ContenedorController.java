package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ContenedorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContenedorController.class);
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
            + "       c.lleno, "
            + "       c.bloqueo, "
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
                    JsonObject c = new JsonObject();
                    try {
                        c.put("id", row.getInteger("contenedor_id"));
                        c.put("nombre", row.getString("contenedor_nombre"));
                        c.put("capacidad_maxima", row.getFloat("capacidad_maxima"));
                        c.put("carga_actual", row.getFloat("carga_actual"));
                        c.put("lleno", row.getBoolean("lleno"));
                        c.put("bloqueo", row.getBoolean("bloqueo"));
                        c.put("lat", row.getDouble("lat"));
                        c.put("lon", row.getDouble("lon"));
                        JsonObject zona = new JsonObject()
                            .put("id", row.getInteger("zona_id"))
                            .put("nombre", row.getString("zona_nombre"));
                        c.put("zona", zona);
                        arr.add(c);
                    } catch (Exception ex) {
                        // Si alguna fila viene mal (p. ej. null donde no debe), la registramos y la omitimos
                        LOGGER.error("Fila de contenedor inválida, omitiendo. Row: " + row.toJson(), ex);
                    }
                }

                ctx.response()
                   .putHeader("Content-Type", "application/json")
                   .end(new JsonObject().put("contenedores", arr).encodePrettily());
            } else {
                // Si falla la consulta, registramos el error y respondemos 500
                Throwable cause = ar.cause();
                LOGGER.error("Error al listar contenedores", cause);
                ctx.response()
                   .setStatusCode(500)
                   .end("❌ Error al obtener contenedores: " + cause.getMessage());
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
            + "       c.lleno, "
            + "       c.bloqueo, "
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
                    try {
                        JsonObject c = new JsonObject()
                            .put("id", row.getInteger("contenedor_id"))
                            .put("nombre", row.getString("contenedor_nombre"))
                            .put("capacidad_maxima", row.getFloat("capacidad_maxima"))
                            .put("carga_actual", row.getFloat("carga_actual"))
                            .put("lleno", row.getBoolean("lleno"))
                            .put("bloqueo", row.getBoolean("bloqueo"))
                            .put("lat", row.getDouble("lat"))
                            .put("lon", row.getDouble("lon"))
                            .put("zona", new JsonObject()
                                .put("id", row.getInteger("zona_id"))
                                .put("nombre", row.getString("zona_nombre"))
                            );
                        ctx.response()
                           .putHeader("Content-Type", "application/json")
                           .end(c.encodePrettily());
                    } catch (Exception ex) {
                        LOGGER.error("Error al formatear contenedor id=" + id, ex);
                        ctx.response().setStatusCode(500).end("❌ Error interno al procesar contenedor");
                    }
                }
            } else {
                Throwable cause = ar.cause();
                LOGGER.error("Error al obtener contenedor id=" + id, cause);
                ctx.response()
                   .setStatusCode(500)
                   .end("❌ Error al obtener contenedor: " + cause.getMessage());
            }
        });
    }

    // POST /contenedores
    private void handleCreateContenedor(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("❌ JSON requerido");
            return;
        }

        String nombre = body.getString("nombre");
        Integer zonaId = body.getInteger("zonaId");
        Double lat = body.getDouble("lat");
        Double lon = body.getDouble("lon");
        Float capacidad = body.getFloat("capacidad_maxima", 100f);
        Float carga = body.getFloat("carga_actual", 0f);
        Boolean lleno = carga >= capacidad;

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
                    Throwable cause = ar.cause();
                    LOGGER.error("Error al crear contenedor con body=" + body.encode(), cause);
                    ctx.response()
                       .setStatusCode(500)
                       .end("❌ Error al crear contenedor: " + cause.getMessage());
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

        String nombre = body.getString("nombre");
        Integer zonaId = body.getInteger("zonaId");
        Double lat = body.getDouble("lat");
        Double lon = body.getDouble("lon");
        Float capacidad = body.getFloat("capacidad_maxima");

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
                    Throwable cause = ar.cause();
                    LOGGER.error("Error al actualizar contenedor id=" + id + " con body=" + body.encode(), cause);
                    ctx.response().setStatusCode(500).end("❌ Error al actualizar contenedor: " + cause.getMessage());
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
                Throwable cause = ar.cause();
                LOGGER.error("Error al eliminar contenedor id=" + id, cause);
                ctx.response().setStatusCode(500).end("❌ Error al eliminar contenedor: " + cause.getMessage());
            }
        });
    }
}
