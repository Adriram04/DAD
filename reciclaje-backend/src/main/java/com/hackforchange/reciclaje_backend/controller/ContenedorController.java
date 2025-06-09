// ContenedorController.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import com.hackforchange.reciclaje_backend.service.ContenedorService;
import com.hackforchange.reciclaje_backend.repository.ContenedorRepository;
import io.vertx.mysqlclient.MySQLPool;

/**
 * Controller que gestiona los endpoints CRUD para contenedores.
 * Se apoya en {@link ContenedorService} para la lógica de negocio
 * y en {@link ContenedorRepository} para el acceso a datos.
 */
public class ContenedorController {

    /** Servicio de contenedores que orquesta la lógica de negocio */
    private final ContenedorService service;

    /**
     * Constructor que inicializa el servicio con el repositorio.
     *
     * @param client Pool de conexiones MySQL proporcionado por el verticle principal.
     */
    public ContenedorController(MySQLPool client) {
        this.service = new ContenedorService(new ContenedorRepository(client));
    }

    /**
     * Registra todas las rutas relacionadas con contenedores en el router dado.
     *
     * @param router Instancia de Vert.x Router donde se montarán los endpoints.
     */
    public void getRouter(Router router) {
        router.get("/contenedores").handler(this::listAll);
        router.get("/contenedores/:id").handler(this::getById);
        router.post("/contenedores").handler(this::create);
        router.put("/contenedores/:id").handler(this::update);
        router.delete("/contenedores/:id").handler(this::delete);
    }

    /**
     * GET /contenedores
     * Obtiene todos los contenedores.
     *
     * @param ctx Contexto de la petición HTTP.
     */
    private void listAll(RoutingContext ctx) {
        service.listAll()
            .onSuccess(res -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(res.encodePrettily()))
            .onFailure(err -> ctx.response()
                .setStatusCode(500)
                .end("❌ Error al obtener contenedores: " + err.getMessage()));
    }

    /**
     * GET /contenedores/:id
     * Obtiene un contenedor por su ID.
     *
     * @param ctx Contexto de la petición HTTP.
     */
    private void getById(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ ID inválido");
            return;
        }

        service.getById(id)
            .onSuccess(res -> {
                if (res == null) {
                    ctx.response()
                       .setStatusCode(404)
                       .end("❌ Contenedor no encontrado");
                } else {
                    ctx.response()
                       .putHeader("Content-Type", "application/json")
                       .end(res.encodePrettily());
                }
            })
            .onFailure(err -> ctx.response()
                .setStatusCode(500)
                .end("❌ Error al obtener contenedor: " + err.getMessage()));
    }

    /**
     * POST /contenedores
     * Crea un nuevo contenedor a partir del JSON del body.
     *
     * @param ctx Contexto de la petición HTTP.
     */
    private void create(RoutingContext ctx) {
        JsonObject b = ctx.getBodyAsJson();
        String nombre  = b.getString("nombre");
        Integer zonaId = b.getInteger("zonaId");
        Double lat     = b.getDouble("lat");
        Double lon     = b.getDouble("lon");
        Float capMax   = b.getFloat("capacidad_maxima", 100f);
        Float carga    = b.getFloat("carga_actual", 0f);

        // Validación de campos obligatorios
        if (nombre == null || zonaId == null || lat == null || lon == null) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ Faltan campos obligatorios");
            return;
        }

        service.create(nombre, zonaId, lat, lon, capMax, carga)
            .onSuccess(v -> ctx.response()
                .setStatusCode(201)
                .end("✅ Contenedor creado"))
            .onFailure(err -> ctx.response()
                .setStatusCode(500)
                .end("❌ Error al crear contenedor: " + err.getMessage()));
    }

    /**
     * PUT /contenedores/:id
     * Actualiza un contenedor existente.
     *
     * @param ctx Contexto de la petición HTTP.
     */
    private void update(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ ID inválido");
            return;
        }

        JsonObject b = ctx.getBodyAsJson();
        String nombre  = b.getString("nombre");
        Integer zonaId = b.getInteger("zonaId");
        Double lat     = b.getDouble("lat");
        Double lon     = b.getDouble("lon");
        Float capMax   = b.getFloat("capacidad_maxima");

        // Validación de campos obligatorios
        if (nombre == null || zonaId == null || lat == null || lon == null || capMax == null) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ Faltan campos obligatorios");
            return;
        }

        service.update(id, nombre, zonaId, lat, lon, capMax)
            .onSuccess(rows -> {
                if (rows == 0) {
                    ctx.response()
                       .setStatusCode(404)
                       .end("❌ Contenedor no encontrado");
                } else {
                    ctx.response().end("✅ Contenedor actualizado");
                }
            })
            .onFailure(err -> ctx.response()
                .setStatusCode(500)
                .end("❌ Error al actualizar contenedor: " + err.getMessage()));
    }

    /**
     * DELETE /contenedores/:id
     * Elimina un contenedor por su ID.
     *
     * @param ctx Contexto de la petición HTTP.
     */
    private void delete(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response()
               .setStatusCode(400)
               .end("❌ ID inválido");
            return;
        }

        service.delete(id)
            .onSuccess(rows -> {
                if (rows == 0) {
                    ctx.response()
                       .setStatusCode(404)
                       .end("❌ Contenedor no encontrado");
                } else {
                    ctx.response().end("✅ Contenedor eliminado");
                }
            })
            .onFailure(err -> ctx.response()
                .setStatusCode(500)
                .end("❌ Error al eliminar contenedor: " + err.getMessage()));
    }
}
