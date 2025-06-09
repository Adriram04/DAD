// ZonaRepository.java
package com.hackforchange.reciclaje_backend.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.mysqlclient.MySQLPool;

/**
 * Repository encargado de todas las operaciones CRUD sobre la tabla `zona`
 * y la tabla intermedia `usuario_zona`, así como de consultas relacionadas
 * con contenedores para agregar información de contenedores a las zonas.
 */
public class ZonaRepository {
    private final MySQLPool client;

    public ZonaRepository(MySQLPool client) {
        this.client = client;
    }

    /**
     * Recupera todas las zonas junto con su geometría.
     * @return Future con RowSet de filas (id, nombre, geom).
     */
    public Future<RowSet<Row>> findAllGeo() {
        Promise<RowSet<Row>> promise = Promise.promise();
        String sql = "SELECT id, nombre, geom FROM zona";
        client.query(sql).execute(ar -> {
            if (ar.succeeded()) promise.complete(ar.result());
            else                promise.fail(ar.cause());
        });
        return promise.future();
    }

    /**
     * Recupera todas las zonas básicas (solo id y nombre).
     * @return Future con RowSet de filas (id, nombre).
     */
    public Future<RowSet<Row>> findAllBasic() {
        Promise<RowSet<Row>> promise = Promise.promise();
        String sql = "SELECT id, nombre FROM zona";
        client.query(sql).execute(ar -> {
            if (ar.succeeded()) promise.complete(ar.result());
            else                promise.fail(ar.cause());
        });
        return promise.future();
    }

    /**
     * Recupera todos los contenedores para luego agregarlos a su zona correspondiente.
     * @return Future con RowSet de filas (id, nombre, id_zona, lleno, bloqueo).
     */
    public Future<RowSet<Row>> findContenedores() {
        Promise<RowSet<Row>> promise = Promise.promise();
        String sql = "SELECT id, nombre, id_zona, lleno, bloqueo FROM contenedor";
        client.query(sql).execute(ar -> {
            if (ar.succeeded()) promise.complete(ar.result());
            else                promise.fail(ar.cause());
        });
        return promise.future();
    }

    /**
     * Inserta una nueva zona con nombre, canal MQTT y geometría.
     * @param nombre Nombre de la zona.
     * @param geom   Geometría almacenada como JsonArray.
     * @return Future completado al insertar.
     */
    public Future<Void> insertZona(String nombre, JsonArray geom) {
        Promise<Void> promise = Promise.promise();
        String canal = "zona/" + nombre.toLowerCase().replace(" ", "_");
        String sql   = "INSERT INTO zona (nombre, canal_mqtt, geom) VALUES (?,?,?)";
        client.preparedQuery(sql)
              .execute(Tuple.of(nombre, canal, geom), ar -> {
                  if (ar.succeeded()) promise.complete();
                  else                promise.fail(ar.cause());
              });
        return promise.future();
    }

    /**
     * Actualiza nombre y geometría de una zona.
     * @param id     Identificador de la zona.
     * @param nombre Nuevo nombre.
     * @param geom   Nueva geometría.
     * @return Future con número de filas afectadas.
     */
    public Future<Integer> updateZona(int id, String nombre, JsonArray geom) {
        Promise<Integer> promise = Promise.promise();
        String sql = "UPDATE zona SET nombre = ?, geom = ? WHERE id = ?";
        client.preparedQuery(sql)
              .execute(Tuple.of(nombre, geom, id), ar -> {
                  if (ar.succeeded()) promise.complete(ar.result().rowCount());
                  else                promise.fail(ar.cause());
              });
        return promise.future();
    }

    /**
     * Elimina una zona por su ID.
     * @param id Identificador de la zona.
     * @return Future con número de filas eliminadas.
     */
    public Future<Integer> deleteZona(int id) {
        Promise<Integer> promise = Promise.promise();
        String sql = "DELETE FROM zona WHERE id = ?";
        client.preparedQuery(sql)
              .execute(Tuple.of(id), ar -> {
                  if (ar.succeeded()) promise.complete(ar.result().rowCount());
                  else                promise.fail(ar.cause());
              });
        return promise.future();
    }

    /**
     * Asigna una zona a un usuario en la tabla intermedia usuario_zona.
     * @param usuarioId Identificador del usuario.
     * @param zonaId    Identificador de la zona.
     * @return Future completado al insertar la asignación.
     */
    public Future<Void> assignUsuarioZona(int usuarioId, int zonaId) {
        Promise<Void> promise = Promise.promise();
        String sql = "INSERT INTO usuario_zona (id_usuario, id_zona) VALUES (?, ?)";
        client.preparedQuery(sql)
              .execute(Tuple.of(usuarioId, zonaId), ar -> {
                  if (ar.succeeded()) promise.complete();
                  else                promise.fail(ar.cause());
              });
        return promise.future();
    }
}
