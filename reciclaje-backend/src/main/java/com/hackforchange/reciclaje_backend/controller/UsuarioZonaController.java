package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Tuple;

/**
 * Controlador dedicado a gestionar las asignaciones
 * entre usuarios y zonas (tabla usuario_zona).
 */
public class UsuarioZonaController {

    private final MySQLPool client;

    public UsuarioZonaController(MySQLPool client) {
        this.client = client;
    }

    /**
     * Registra la ruta POST /usuario_zona (sin prefijo).
     * Espera un JSON: { "usuarioId": <String o Number>, "zonaId": <String o Number> }.
     */
    public void getRouter(Router router) {
        router.post("/usuario_zona").handler(this::handleAsignarZona);
    }

    /**
     * Inserta una fila en la tabla usuario_zona (columna id_usuario, id_zona).
     * Si el body o los campos no están bien, devuelve 400.
     * Si la inserción falla, devuelve 500.
     * En caso de éxito, responde 201 con un JSON { message: "✅ Asignación guardada" }.
     */
    private void handleAsignarZona(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("❌ JSON requerido");
            return;
        }

        // Leemos primero como String para luego parsear a Integer
        String usuarioIdStr = body.getString("usuarioId");
        String zonaIdStr    = body.getString("zonaId");

        if (usuarioIdStr == null || usuarioIdStr.isBlank() ||
            zonaIdStr == null    || zonaIdStr.isBlank()) {
            ctx.response().setStatusCode(400).end("❌ usuarioId y zonaId son obligatorios");
            return;
        }

        Integer usuarioId;
        Integer zonaId;
        try {
            usuarioId = Integer.parseInt(usuarioIdStr);
            zonaId    = Integer.parseInt(zonaIdStr);
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ usuarioId y zonaId deben ser números válidos");
            return;
        }

        String sql = "INSERT INTO usuario_zona (id_usuario, id_zona) VALUES (?, ?)";

        client.preparedQuery(sql)
              .execute(Tuple.of(usuarioId, zonaId), ar -> {
                  if (ar.succeeded()) {
                      ctx.response()
                         .setStatusCode(201)
                         .putHeader("Content-Type", "application/json")
                         .end(new JsonObject()
                             .put("message", "✅ Asignación guardada")
                             .encodePrettily());
                  } else {
                      ctx.response()
                         .setStatusCode(500)
                         .end("❌ Error al asignar zona: " + ar.cause().getMessage());
                  }
              });
    }
}
