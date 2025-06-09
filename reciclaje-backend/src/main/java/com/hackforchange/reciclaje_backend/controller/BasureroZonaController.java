// BasureroZonaController.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import com.hackforchange.reciclaje_backend.service.BasureroZonaService;
import com.hackforchange.reciclaje_backend.repository.BasureroZonaRepository;
import io.vertx.mysqlclient.MySQLPool;

/**
 * Controller que expone el endpoint para listar las zonas asignadas
 * a un basurero (usuario con rol BASURERO).
 *
 * Endpoint: GET /basurero/:id/zonas
 * Devuelve un JSON con la lista de zonas asociadas al basurero.
 */
public class BasureroZonaController {

    /** Servicio que orquesta la lógica de negocio para asignaciones basurero-zona */
    private final BasureroZonaService service;

    /**
     * Constructor que inicializa el servicio usando el repositorio correspondiente.
     *
     * @param client Pool de conexiones MySQL para el repositorio
     */
    public BasureroZonaController(MySQLPool client) {
        this.service = new BasureroZonaService(new BasureroZonaRepository(client));
    }

    /**
     * Registra la ruta GET /basurero/:id/zonas en el router proporcionado.
     *
     * @param router Instancia de Vert.x Router donde se montará la ruta
     */
    public void getRouter(Router router) {
        router.get("/basurero/:id/zonas").handler(this::handleListZonas);
    }

    /**
     * Manejador de la petición GET /basurero/:id/zonas.
     * Valida que el parámetro ID sea numérico, luego delega en el servicio
     * para recuperar las zonas asociadas al basurero.
     *
     * @param ctx Contexto de la petición HTTP
     */
    private void handleListZonas(RoutingContext ctx) {
        String idStr = ctx.pathParam("id");
        int usuarioId;

        // Validar que el ID sea un entero válido
        try {
            usuarioId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ ID de basurero inválido");
            return;
        }

        // Llamada al servicio para obtener zonas
        service.listZonas(usuarioId)
            .onSuccess(result -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(result.encodePrettily()))
            .onFailure(err -> ctx.response()
                .setStatusCode(500)
                .end("❌ Error al obtener zonas: " + err.getMessage()));
    }
}
