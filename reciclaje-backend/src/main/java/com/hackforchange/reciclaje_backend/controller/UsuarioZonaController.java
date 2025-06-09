// UsuarioZonaController.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import com.hackforchange.reciclaje_backend.service.ZonaService;
import com.hackforchange.reciclaje_backend.repository.ZonaRepository;
import io.vertx.mysqlclient.MySQLPool;

/**
 * Controller responsable de gestionar las asignaciones entre usuarios y zonas.
 * Expone un endpoint POST /usuario_zona que crea una relación en la tabla usuario_zona.
 */
public class UsuarioZonaController {

    /** Servicio que maneja la lógica de negocio relacionada con zonas */
    private final ZonaService service;

    /**
     * Constructor.
     *
     * @param client Pool de conexiones MySQL para inicializar el repositorio de zona.
     */
    public UsuarioZonaController(MySQLPool client) {
        this.service = new ZonaService(new ZonaRepository(client));
    }

    /**
     * Registra la ruta POST /usuario_zona en el router proporcionado.
     * Se aplica un BodyHandler para parsear el cuerpo como JSON.
     *
     * @param router Instancia de Router de Vert.x donde se montan las rutas.
     */
    public void getRouter(Router router) {
        router.post("/usuario_zona")
              .handler(BodyHandler.create())
              .handler(this::assign);
    }

    /**
     * Manejador de la petición POST /usuario_zona.
     * Valida que los campos usuarioId y zonaId estén presentes y sean numéricos,
     * luego delega en el servicio para crear la asignación.
     *
     * @param ctx Contexto de la petición HTTP de Vert.x.
     */
    private void assign(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        String uStr = body.getString("usuarioId");
        String zStr = body.getString("zonaId");

        // Validación de presencia de campos
        if (uStr == null || zStr == null) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ usuarioId y zonaId son obligatorios");
            return;
        }

        try {
            // Conversión a enteros
            int usuarioId = Integer.parseInt(uStr);
            int zonaId    = Integer.parseInt(zStr);

            // Llamada al servicio para asignar zona
            service.assignUsuarioZona(usuarioId, zonaId)
                .onSuccess(v -> {
                    // Respuesta 201 en caso de éxito
                    ctx.response()
                       .setStatusCode(201)
                       .end(new JsonObject()
                           .put("message", "✅ Asignación guardada")
                           .encode());
                })
                .onFailure(err -> {
                    // Error inesperado al insertar en BD
                    ctx.response()
                       .setStatusCode(500)
                       .end("❌ Error al asignar zona: " + err.getMessage());
                });
        } catch (NumberFormatException ex) {
            // Manejo de formato de número inválido
            ctx.response()
               .setStatusCode(400)
               .end("❌ usuarioId y zonaId deben ser números válidos");
        }
    }
}
