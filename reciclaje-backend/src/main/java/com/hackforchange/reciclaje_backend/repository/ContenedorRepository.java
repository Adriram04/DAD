// ContenedorRepository.java
package com.hackforchange.reciclaje_backend.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.mysqlclient.MySQLPool;

/**
 * Repositorio responsable de las operaciones CRUD sobre la tabla `contenedor`.
 * Todas las consultas devuelven Future para integración asíncrona con Vert.x.
 */
public class ContenedorRepository {
    /** Pool de conexiones MySQL proporcionado por el verticle principal */
    private final MySQLPool client;

    /**
     * Constructor.
     *
     * @param client Pool de conexiones MySQL
     */
    public ContenedorRepository(MySQLPool client) {
        this.client = client;
    }

    /**
     * Recupera todos los contenedores junto con su zona asociada.
     *
     * @return Future con RowSet de filas que contienen
     *         campos aliased: contenedor_id, contenedor_nombre, capacidad_maxima, carga_actual,
     *         lleno, bloqueo, lat, lon, zona_id, zona_nombre.
     */
    public Future<RowSet<Row>> findAll() {
        Promise<RowSet<Row>> promise = Promise.promise();
        String sql =
            "SELECT c.id AS contenedor_id, c.nombre AS contenedor_nombre, " +
            "c.capacidad_maxima, c.carga_actual, c.lleno, c.bloqueo, " +
            "c.lat, c.lon, z.id AS zona_id, z.nombre AS zona_nombre " +
            "FROM contenedor c JOIN zona z ON c.id_zona = z.id";
        client.query(sql).execute(ar -> {
            if (ar.succeeded()) promise.complete(ar.result());
            else                promise.fail(ar.cause());
        });
        return promise.future();
    }

    /**
     * Recupera un contenedor por su ID, incluyendo la información de zona.
     *
     * @param id Identificador del contenedor
     * @return Future con la primera fila encontrada o null si no existe
     */
    public Future<Row> findById(int id) {
        Promise<Row> promise = Promise.promise();
        String sql =
            "SELECT c.id AS contenedor_id, c.nombre AS contenedor_nombre, " +
            "c.capacidad_maxima, c.carga_actual, c.lleno, c.bloqueo, " +
            "c.lat, c.lon, z.id AS zona_id, z.nombre AS zona_nombre " +
            "FROM contenedor c JOIN zona z ON c.id_zona = z.id WHERE c.id = ?";
        client.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                // Retorna la primera fila o null si no hay resultados
                RowSet<Row> rs = ar.result();
                promise.complete(rs.iterator().hasNext() ? rs.iterator().next() : null);
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    /**
     * Inserta un nuevo contenedor en la base de datos.
     *
     * @param nombre  Nombre del contenedor
     * @param zonaId  ID de la zona relacionada
     * @param lat     Latitud del contenedor
     * @param lon     Longitud del contenedor
     * @param capMax  Capacidad máxima
     * @param carga   Carga inicial
     * @param lleno   Estado de lleno calculado
     * @return Future completado cuando la inserción finalice
     */
    public Future<Void> insert(String nombre, int zonaId, double lat, double lon,
                               float capMax, float carga, boolean lleno) {
        Promise<Void> promise = Promise.promise();
        String sql = "INSERT INTO contenedor " +
                     "(nombre, id_zona, lat, lon, capacidad_maxima, carga_actual, lleno) " +
                     "VALUES (?,?,?,?,?,?,?)";
        client.preparedQuery(sql).execute(
            Tuple.of(nombre, zonaId, lat, lon, capMax, carga, lleno),
            ar -> {
                if (ar.succeeded()) promise.complete();
                else                promise.fail(ar.cause());
            });
        return promise.future();
    }

    /**
     * Actualiza los campos de un contenedor existente.
     *
     * @param id      Identificador del contenedor a actualizar
     * @param nombre  Nuevo nombre
     * @param zonaId  Nuevo ID de zona
     * @param lat     Nueva latitud
     * @param lon     Nueva longitud
     * @param capMax  Nueva capacidad máxima
     * @return Future con el número de filas afectadas
     */
    public Future<Integer> update(int id, String nombre, int zonaId,
                                  double lat, double lon, float capMax) {
        Promise<Integer> promise = Promise.promise();
        String sql = "UPDATE contenedor SET " +
                     "nombre = ?, id_zona = ?, lat = ?, lon = ?, capacidad_maxima = ? " +
                     "WHERE id = ?";
        client.preparedQuery(sql).execute(
            Tuple.of(nombre, zonaId, lat, lon, capMax, id),
            ar -> {
                if (ar.succeeded()) promise.complete(ar.result().rowCount());
                else                promise.fail(ar.cause());
            });
        return promise.future();
    }

    /**
     * Elimina un contenedor por su ID.
     *
     * @param id Identificador del contenedor
     * @return Future con el número de filas eliminadas
     */
    public Future<Integer> delete(int id) {
        Promise<Integer> promise = Promise.promise();
        client.preparedQuery("DELETE FROM contenedor WHERE id = ?")
            .execute(Tuple.of(id), ar -> {
                if (ar.succeeded()) promise.complete(ar.result().rowCount());
                else                promise.fail(ar.cause());
            });
        return promise.future();
    }
}
