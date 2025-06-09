// GeoController.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import com.hackforchange.reciclaje_backend.service.ZonaService;
import com.hackforchange.reciclaje_backend.repository.ZonaRepository;
import io.vertx.mysqlclient.MySQLPool;

/**
 * Controller que gestiona los endpoints de zona relacionados con geolocalización:
 *  - GET    /zonas/geo
 *  - POST   /zonas
 *  - PUT    /zonas/:id
 *  - DELETE /zonas/:id
 *
 * Utiliza {@link ZonaService} para la lógica de negocio y
 * {@link ZonaRepository} para el acceso a datos.
 */
public class GeoController {

    /** Servicio de negocio para operaciones de Zonas */
    private final ZonaService service;

    /**
     * Constructor que inyecta el pool de MySQL y crea el servicio.
     *
     * @param client Pool de conexiones MySQL
     */
    public GeoController(MySQLPool client) {
        this.service = new ZonaService(new ZonaRepository(client));
    }

    /**
     * Registra las rutas en el router proporcionado.
     *
     * @param router Instancia de Router de Vert.x
     */
    public void getRouter(Router router) {
        router.get("/zonas/geo").handler(this::listGeo);
        router.post("/zonas")
              .handler(BodyHandler.create())
              .handler(this::createZona);
        router.put("/zonas/:id")
              .handler(BodyHandler.create())
              .handler(this::updateZona);
        router.delete("/zonas/:id").handler(this::deleteZona);
    }

    /**
     * Manejador de GET /zonas/geo.
     * Recupera todas las zonas con su geometría y devuelve un JSON.
     *
     * @param ctx Contexto de la petición HTTP
     */
    private void listGeo(RoutingContext ctx) {
        service.listGeo()
            .onSuccess(zonas -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(zonas.encodePrettily()))
            .onFailure(err -> ctx.response()
                .setStatusCode(500)
                .end("❌ Error obteniendo zonas: " + err.getMessage()));
    }

    /**
     * Manejador de POST /zonas.
     * Valida el body para nombre y geometría, luego crea la zona.
     *
     * @param ctx Contexto de la petición HTTP
     */
    private void createZona(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        String nombre = body.getString("nombre");
        JsonArray geom = body.getJsonArray("geom");

        // Validación de entrada
        if (nombre == null || geom == null) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ 'nombre' y 'geom' son obligatorios");
            return;
        }

        // Llamada al servicio para crear la zona
        service.createZona(nombre, geom)
            .onSuccess(v -> ctx.response()
                .setStatusCode(201)
                .end("✅ Zona creada"))
            .onFailure(err -> ctx.response()
                .setStatusCode(500)
                .end("❌ Error creando zona: " + err.getMessage()));
    }

    /**
     * Manejador de PUT /zonas/:id.
     * Valida ID, nombre y geometría, luego actualiza la zona indicada.
     *
     * @param ctx Contexto de la petición HTTP
     */
    private void updateZona(RoutingContext ctx) {
        // Parseo y validación de ID
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException ex) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ ID inválido");
            return;
        }

        JsonObject body = ctx.getBodyAsJson();
        String nombre = body.getString("nombre");
        JsonArray geom = body.getJsonArray("geom");

        // Validación de entrada
        if (nombre == null || geom == null) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ 'nombre' y 'geom' son obligatorios");
            return;
        }

        // Llamada al servicio para actualizar la zona
        service.updateZona(id, nombre, geom)
            .onSuccess(v -> ctx.response()
                .setStatusCode(200)
                .end("✅ Zona actualizada"))
            .onFailure(err -> {
                // Si no se encontró la zona, devolvemos 404
                int code = err.getMessage().contains("no encontrada") ? 404 : 500;
                ctx.response()
                   .setStatusCode(code)
                   .end("❌ " + err.getMessage());
            });
    }

    /**
     * Manejador de DELETE /zonas/:id.
     * Valida ID y elimina la zona correspondiente.
     *
     * @param ctx Contexto de la petición HTTP
     */
    private void deleteZona(RoutingContext ctx) {
        // Parseo y validación de ID
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException ex) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ ID inválido");
            return;
        }

        // Llamada al servicio para eliminar la zona
        service.deleteZona(id)
            .onSuccess(v -> ctx.response()
                .setStatusCode(200)
                .end("✅ Zona eliminada"))
            .onFailure(err -> {
                // Si no se encontró la zona, devolvemos 404
                int code = err.getMessage().contains("no encontrada") ? 404 : 500;
                ctx.response()
                   .setStatusCode(code)
                   .end("❌ " + err.getMessage());
            });
    }
}
