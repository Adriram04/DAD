package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class ContenedorController {

    private final MySQLPool client;

    public ContenedorController(MySQLPool client) {
        this.client = client;
    }

    public void getRouter(Router router) {
        router.get("/contenedores").handler(this::handleListContenedores);
        router.post("/contenedores").handler(this::handleCreateContenedor);
        router.put("/contenedores/:id").handler(this::handleUpdateContenedor);
        router.delete("/contenedores/:id").handler(this::handleDeleteContenedor);
    }

    private void handleListContenedores(RoutingContext ctx) {
        String query = "SELECT c.id AS contenedor_id, c.nombre AS contenedor_nombre, " +
                       "c.capacidad_maxima, c.carga_actual, z.id AS zona_id, z.nombre AS zona_nombre " +
                       "FROM contenedor c JOIN zona z ON c.id_zona = z.id";

        client.query(query).execute(ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                JsonArray contenedores = new JsonArray();

                for (Row row : rows) {
                    float capacidadMax = row.getFloat("capacidad_maxima");
                    float cargaActual = row.getFloat("carga_actual");
                    boolean lleno = cargaActual< capacidadMax && capacidadMax*0.75 <= cargaActual;
                    boolean bloqueo = cargaActual >= capacidadMax;

                    JsonObject contenedor = new JsonObject()
                        .put("id", row.getInteger("contenedor_id"))
                        .put("nombre", row.getString("contenedor_nombre"))
                        .put("capacidad_maxima", capacidadMax)
                        .put("carga_actual", cargaActual)
                        .put("lleno", lleno)
                        .put("bloqueo", bloqueo)
                        .put("zona", new JsonObject()
                            .put("id", row.getInteger("zona_id"))
                            .put("nombre", row.getString("zona_nombre"))
                        );

                    contenedores.add(contenedor);
                }

                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("contenedores", contenedores).encodePrettily());
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al obtener contenedores");
            }
        });
    }


    private void handleCreateContenedor(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String nombre = body.getString("nombre");
        System.out.println(body);
        int zonaId;
        try {
        	zonaId = body.getInteger("zonaId");
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end("❌ zonaId inválido o no proporcionado");
            return;
        }

        if (nombre == null || zonaId == 0) {
            ctx.response().setStatusCode(400).end("❌ Faltan datos del contenedor");
            return;
        }

        String sql = "INSERT INTO contenedor (nombre, id_zona, capacidad_maxima, carga_actual, lleno) VALUES (?, ?, ?, ?, ?)";
        float capacidad = body.getFloat("capacidad_maxima", 100f);
        float carga = body.getFloat("carga_actual", 0f);
        boolean lleno = carga >= capacidad;

        client.preparedQuery(sql).execute(
            Tuple.of(nombre, zonaId, capacidad, carga, lleno),
            ar -> {
                if (ar.succeeded()) {
                    ctx.response().setStatusCode(201).end("✅ Contenedor creado");
                } else {
                    ctx.response().setStatusCode(500).end("❌ Error al crear contenedor: " + ar.cause().getMessage());
                }
            });

    }

    private void handleUpdateContenedor(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject body = ctx.body().asJsonObject();
        String nombre = body.getString("nombre");
        int zonaId;
        float capacidadMaxima;

        try {
            zonaId = body.getInteger("zonaId");
            capacidadMaxima = body.getFloat("capacidad_maxima");
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end("❌ zonaId o capacidad inválida");
            return;
        }

        String sql = "UPDATE contenedor SET nombre = ?, id_zona = ?, capacidad_maxima = ? WHERE id = ?";
        client.preparedQuery(sql).execute(Tuple.of(nombre, zonaId, capacidadMaxima, id), ar -> {
            if (ar.succeeded()) {
                ctx.response().end("✅ Contenedor actualizado");
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al actualizar contenedor");
            }
        });
    }


    private void handleDeleteContenedor(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        client.preparedQuery("DELETE FROM contenedor WHERE id = ?").execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                ctx.response().end("✅ Contenedor eliminado");
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al eliminar contenedor");
            }
        });
    }
}
