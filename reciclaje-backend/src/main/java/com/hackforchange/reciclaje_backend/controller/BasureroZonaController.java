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

/**
 * Controlador para listar las zonas asignadas a un basurero (usuario con rol BASURERO).
 * Expone GET /basurero/:id/zonas
 */
public class BasureroZonaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasureroZonaController.class);
    private final MySQLPool client;

    public BasureroZonaController(MySQLPool client) {
        this.client = client;
    }

    /**
     * Registra la ruta GET /basurero/:id/zonas en el router dado.
     */
    public void getRouter(Router router) {
        router.get("/basurero/:id/zonas").handler(this::handleListZonas);
    }

    /**
     * Maneja la petición GET /basurero/:id/zonas.
     * Devuelve un JSON { zonas: [ { id, nombre, geom }, … ] }
     */
    private void handleListZonas(RoutingContext ctx) {
        String idStr = ctx.pathParam("id");
        Integer basureroId;

        // 1) Validar que el parámetro ID es numérico
        try {
            basureroId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ ID de basurero inválido");
            return;
        }

        // 2) SQL: buscar zonas en usuario_zona unidas con zona
        String sql = ""
            + "SELECT z.id, z.nombre, z.geom "
            + "FROM usuario_zona uz "
            + "JOIN zona z ON uz.id_zona = z.id "
            + "WHERE uz.id_usuario = ?";

        client.preparedQuery(sql)
              .execute(Tuple.of(basureroId), ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                JsonArray arrayZonas = new JsonArray();

                for (Row row : rows) {
                    // 3) Extraer geom como JsonArray, ya sea nativo o como String
                    Object rawGeom = row.getValue("geom");
                    JsonArray geom = null;
                    try {
                        if (rawGeom != null) {
                            if (rawGeom instanceof JsonArray) {
                                geom = (JsonArray) rawGeom;
                            } else {
                                // Si viene como String (por ejemplo: '"[[lat,lon],…]"')
                                geom = new JsonArray(rawGeom.toString());
                            }
                        }
                    } catch (Exception ex) {
                        // Si geom no es un JSON válido, lo dejamos null y registramos
                        LOGGER.error("geom inválido para zona.id=" + row.getInteger("id") + ": " + rawGeom, ex);
                        geom = null;
                    }

                    arrayZonas.add(new JsonObject()
                        .put("id", row.getInteger("id"))
                        .put("nombre", row.getString("nombre"))
                        .put("geom", geom)
                    );
                }

                ctx.response()
                   .putHeader("Content-Type", "application/json")
                   .end(new JsonObject().put("zonas", arrayZonas).encodePrettily());
            } else {
                // 4) Si falla la consulta, respondemos 500 y guardamos el stacktrace
                Throwable cause = ar.cause();
                LOGGER.error("Error al ejecutar la consulta de zonas para basureroId=" + basureroId, cause);
                ctx.response()
                   .setStatusCode(500)
                   .end("❌ Error al obtener zonas: " + cause.getMessage());
            }
        });
    }
}
