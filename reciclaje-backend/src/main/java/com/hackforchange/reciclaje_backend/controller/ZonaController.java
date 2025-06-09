// ZonaController.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import com.hackforchange.reciclaje_backend.service.ZonaService;
import com.hackforchange.reciclaje_backend.repository.ZonaRepository;
import io.vertx.mysqlclient.MySQLPool;

/**
 * Controller que expone operaciones de Zona:
 *  - Listar todas las zonas con geometría (GET /zonas)
 *  - Listar todas las zonas junto con sus contenedores (GET /zonas/with-contenedores)
 *
 * Cada método delega la lógica al servicio {@link ZonaService}, que a su vez
 * utiliza {@link ZonaRepository} para interactuar con la base de datos.
 */
public class ZonaController {

    /** Servicio de negocio para operaciones de zona */
    private final ZonaService service;

    /**
     * Constructor.
     * @param client Pool de conexiones MySQL inyectado desde el verticle principal.
     */
    public ZonaController(MySQLPool client) {
        this.service = new ZonaService(new ZonaRepository(client));
    }

    /**
     * Registra las rutas en el router proporcionado.
     * @param router Instancia de Router de Vert.x donde se montarán los endpoints.
     * @return El mismo router con las rutas añadidas.
     */
    public Router getRouter(Router router) {
        // Ruta para listar zonas con geometría
        router.get("/zonas").handler(this::list);
        // Ruta para listar zonas junto con sus contenedores
        router.get("/zonas/with-contenedores").handler(this::listWithContenedores);
        return router;
    }

    /**
     * Manejador de GET /zonas.
     * Llama a service.listGeo() para obtener un listado de zonas en formato GeoJSON.
     * Responde con JSON o con un error HTTP 500 en caso de fallo.
     *
     * @param ctx Contexto de la petición HTTP de Vert.x.
     */
    private void list(RoutingContext ctx) {
        service.listGeo()
            .onSuccess(res -> {
                // Devuelve directamente el objeto JSON
                ctx.json(res);
            })
            .onFailure(e -> {
                // En caso de error en el servicio, retornamos 500 con mensaje
                ctx.response()
                   .setStatusCode(500)
                   .end("❌ Error listando zonas: " + e.getMessage());
            });
    }

    /**
     * Manejador de GET /zonas/with-contenedores.
     * Llama a service.listWithContenedores() para obtener zonas con sus contenedores anidados.
     * Responde con JSON o con un error HTTP 500 en caso de fallo.
     *
     * @param ctx Contexto de la petición HTTP de Vert.x.
     */
    private void listWithContenedores(RoutingContext ctx) {
        service.listWithContenedores()
            .onSuccess(res -> {
                // Devuelve directamente el objeto JSON
                ctx.json(res);
            })
            .onFailure(e -> {
                // Error al componer los datos; devolvemos 500
                ctx.response()
                   .setStatusCode(500)
                   .end("❌ Error listando zonas con contenedores: " + e.getMessage());
            });
    }
}
