// ContenedorService.java
package com.hackforchange.reciclaje_backend.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import com.hackforchange.reciclaje_backend.repository.ContenedorRepository;

/**
 * Servicio que orquesta la lógica de negocio para operaciones CRUD de contenedores.
 * Se encarga de transformar los datos de la capa de repositorio en estructuras JSON
 * adecuadas para la capa de controlador.
 */
public class ContenedorService {
    private final ContenedorRepository repo;

    /**
     * Constructor.
     *
     * @param repo Repositorio de contenedores para acceso a datos.
     */
    public ContenedorService(ContenedorRepository repo) {
        this.repo = repo;
    }

    /**
     * Obtiene todos los contenedores y los formatea en un JSON.
     *
     * @return Future que se completa con un JsonObject { "contenedores": [ ... ] }.
     */
    public Future<JsonObject> listAll() {
        Promise<JsonObject> promise = Promise.promise();
        repo.findAll().onSuccess(rows -> {
            JsonArray arr = new JsonArray();
            for (Row row : rows) {
                JsonObject c = new JsonObject()
                    .put("id",                row.getInteger("contenedor_id"))
                    .put("nombre",            row.getString("contenedor_nombre"))
                    .put("capacidad_maxima",  row.getFloat("capacidad_maxima"))
                    .put("carga_actual",      row.getFloat("carga_actual"))
                    .put("lleno",             row.getBoolean("lleno"))
                    .put("bloqueo",           row.getBoolean("bloqueo"))
                    .put("lat",               row.getDouble("lat"))
                    .put("lon",               row.getDouble("lon"))
                    .put("zona", new JsonObject()
                        .put("id",     row.getInteger("zona_id"))
                        .put("nombre", row.getString("zona_nombre"))
                    );
                arr.add(c);
            }
            promise.complete(new JsonObject().put("contenedores", arr));
        }).onFailure(promise::fail);
        return promise.future();
    }

    /**
     * Obtiene un contenedor por su ID y lo devuelve como JSON.
     *
     * @param id Identificador del contenedor.
     * @return Future que se completa con el JsonObject del contenedor, o null si no existe.
     */
    public Future<JsonObject> getById(int id) {
        Promise<JsonObject> promise = Promise.promise();
        repo.findById(id).onSuccess(row -> {
            if (row == null) {
                // No se encontró el contenedor
                promise.complete();
            } else {
                JsonObject c = new JsonObject()
                    .put("id",                row.getInteger("contenedor_id"))
                    .put("nombre",            row.getString("contenedor_nombre"))
                    .put("capacidad_maxima",  row.getFloat("capacidad_maxima"))
                    .put("carga_actual",      row.getFloat("carga_actual"))
                    .put("lleno",             row.getBoolean("lleno"))
                    .put("bloqueo",           row.getBoolean("bloqueo"))
                    .put("lat",               row.getDouble("lat"))
                    .put("lon",               row.getDouble("lon"))
                    .put("zona", new JsonObject()
                        .put("id",     row.getInteger("zona_id"))
                        .put("nombre", row.getString("zona_nombre"))
                    );
                promise.complete(c);
            }
        }).onFailure(promise::fail);
        return promise.future();
    }

    /**
     * Crea un nuevo contenedor.
     *
     * @param nombre  Nombre del contenedor.
     * @param zonaId  ID de la zona asociada.
     * @param lat     Latitud.
     * @param lon     Longitud.
     * @param capMax  Capacidad máxima.
     * @param carga   Carga inicial.
     * @return Future que se completa cuando se inserta el contenedor.
     */
    public Future<Void> create(String nombre, int zonaId, double lat, double lon,
                               float capMax, float carga) {
        boolean lleno = carga >= capMax;
        return repo.insert(nombre, zonaId, lat, lon, capMax, carga, lleno);
    }

    /**
     * Actualiza un contenedor existente.
     *
     * @param id      Identificador del contenedor.
     * @param nombre  Nuevo nombre.
     * @param zonaId  ID de la zona.
     * @param lat     Nueva latitud.
     * @param lon     Nueva longitud.
     * @param capMax  Nueva capacidad máxima.
     * @return Future que retorna el número de filas afectadas.
     */
    public Future<Integer> update(int id, String nombre, int zonaId,
                                  double lat, double lon, float capMax) {
        return repo.update(id, nombre, zonaId, lat, lon, capMax);
    }

    /**
     * Elimina un contenedor por su ID.
     *
     * @param id Identificador del contenedor.
     * @return Future que retorna el número de filas eliminadas.
     */
    public Future<Integer> delete(int id) {
        return repo.delete(id);
    }
}
