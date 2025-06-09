// BasureroZonaService.java
package com.hackforchange.reciclaje_backend.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import com.hackforchange.reciclaje_backend.repository.BasureroZonaRepository;

/**
 * Servicio encargado de la lógica de negocio para listar las zonas asignadas
 * a un basurero (usuario con rol BASURERO).  
 * Utiliza {@link BasureroZonaRepository} para acceder a la base de datos.
 */
public class BasureroZonaService {

    /** Repositorio para consultas basurero-zona */
    private final BasureroZonaRepository repo;

    /**
     * Constructor.
     *
     * @param repo Repositorio que maneja las consultas de zonas por usuario
     */
    public BasureroZonaService(BasureroZonaRepository repo) {
        this.repo = repo;
    }

    /**
     * Obtiene las zonas asignadas a un basurero dado su ID.
     *
     * @param usuarioId Identificador del basurero (usuario)
     * @return Future que se completa con un JsonObject de la forma:
     *         { "zonas": [ { "id": ..., "nombre": ..., "geom": [...] }, ... ] }
     */
    public Future<JsonObject> listZonas(int usuarioId) {
        Promise<JsonObject> promise = Promise.promise();

        // Consultar zonas asociadas en el repositorio
        repo.findZonasByUsuarioId(usuarioId)
            .onSuccess(rows -> {
                JsonArray zonas = new JsonArray();

                // Iterar sobre cada fila devuelta
                for (Row row : rows) {
                    // Extraer y parsear la geometría (puede venir como String o JsonArray)
                    Object rawGeom = row.getValue("geom");
                    JsonArray geom = null;
                    try {
                        if (rawGeom instanceof JsonArray) {
                            geom = (JsonArray) rawGeom;
                        } else if (rawGeom != null) {
                            geom = new JsonArray(rawGeom.toString());
                        }
                    } catch (Exception ignore) {
                        // Si falla el parseo, dejar geom en null
                    }

                    // Construir objeto JSON de zona
                    JsonObject z = new JsonObject()
                        .put("id",     row.getInteger("id"))
                        .put("nombre", row.getString("nombre"))
                        .put("geom",   geom);

                    zonas.add(z);
                }

                // Completar futuro con el resultado
                promise.complete(new JsonObject().put("zonas", zonas));
            })
            .onFailure(promise::fail);

        return promise.future();
    }
}
