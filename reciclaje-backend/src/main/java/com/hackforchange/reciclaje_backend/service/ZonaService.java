// ZonaService.java
package com.hackforchange.reciclaje_backend.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import com.hackforchange.reciclaje_backend.repository.ZonaRepository;

/**
 * Service que orquesta la lógica de negocio para Zonas,
 * delegando las consultas a ZonaRepository y formateando respuestas JSON.
 */
public class ZonaService {
    private final ZonaRepository repo;

    public ZonaService(ZonaRepository repo) {
        this.repo = repo;
    }

    /**
     * Construye respuesta para GET /zonas/geo.
     * @return Future con objeto JSON que contiene el array de zonas con geometría.
     */
    public Future<JsonObject> listGeo() {
        Promise<JsonObject> promise = Promise.promise();
        repo.findAllGeo().onSuccess(rows -> {
            JsonArray zonas = new JsonArray();
            for (Row r: rows) {
                Object rawGeom = r.getValue("geom");
                JsonArray geom;
                try {
                    geom = rawGeom instanceof JsonArray
                           ? (JsonArray) rawGeom
                           : new JsonArray(rawGeom.toString());
                } catch (Exception e) {
                    geom = null;
                }
                zonas.add(new JsonObject()
                    .put("id",     r.getInteger("id"))
                    .put("nombre", r.getString("nombre"))
                    .put("geom",   geom));
            }
            promise.complete(new JsonObject().put("zonas", zonas));
        }).onFailure(promise::fail);
        return promise.future();
    }

    /**
     * Construye respuesta para GET /zonas/with-contenedores.
     * @return Future con objeto JSON que contiene zonas y sus contenedores.
     */
    public Future<JsonObject> listWithContenedores() {
        Promise<JsonObject> promise = Promise.promise();
        // 1) Obtiene zonas básicas
        repo.findAllBasic().compose(zRes ->
            // 2) Obtiene contenedores y mapea
            repo.findContenedores().map(cRes -> {
                JsonArray zonas = new JsonArray();
                // Inicializa cada zona con array vacío
                zRes.forEach(r -> zonas.add(new JsonObject()
                    .put("id", r.getInteger("id"))
                    .put("nombre", r.getString("nombre"))
                    .put("contenedores", new JsonArray())));
                // Agrega cada contenedor a su zona
                cRes.forEach(c -> {
                    int zid = c.getInteger("id_zona");
                    JsonObject cont = new JsonObject()
                        .put("id", c.getInteger("id"))
                        .put("nombre", c.getString("nombre"))
                        .put("lleno", c.getBoolean("lleno"))
                        .put("bloqueo", c.getBoolean("bloqueo"));
                    for (int i = 0; i < zonas.size(); i++) {
                        JsonObject z = zonas.getJsonObject(i);
                        if (z.getInteger("id") == zid) {
                            z.getJsonArray("contenedores").add(cont);
                            break;
                        }
                    }
                });
                return new JsonObject().put("zonas", zonas);
            })
        ).onSuccess(promise::complete)
         .onFailure(promise::fail);
        return promise.future();
    }

    /** Registra nueva zona. */
    public Future<Void> createZona(String nombre, JsonArray geom) {
        return repo.insertZona(nombre, geom);
    }

    /** Actualiza zona existente; falla si no encuentra ninguna fila. */
    public Future<Void> updateZona(int id, String nombre, JsonArray geom) {
        return repo.updateZona(id, nombre, geom).compose(rows -> {
            if (rows == 0) return Future.failedFuture("Zona no encontrada");
            else           return Future.succeededFuture();
        });
    }

    /** Elimina zona; falla si no existe. */
    public Future<Void> deleteZona(int id) {
        return repo.deleteZona(id).compose(rows -> {
            if (rows == 0) return Future.failedFuture("Zona no encontrada");
            else           return Future.succeededFuture();
        });
    }

    /** Asigna zona a usuario. */
    public Future<Void> assignUsuarioZona(int usuarioId, int zonaId) {
        return repo.assignUsuarioZona(usuarioId, zonaId);
    }
}
