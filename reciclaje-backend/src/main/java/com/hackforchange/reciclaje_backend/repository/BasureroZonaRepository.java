// BasureroZonaRepository.java
package com.hackforchange.reciclaje_backend.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

/**
 * Repositorio encargado de recuperar las zonas asignadas a un basurero
 * (usuario con rol BASURERO) desde la tabla intermedia `usuario_zona`.
 */
public class BasureroZonaRepository {

    /** Pool de conexiones MySQL proporcionado por el verticle principal */
    private final MySQLPool client;

    /**
     * Constructor.
     *
     * @param client Pool de conexiones MySQL para ejecutar queries
     */
    public BasureroZonaRepository(MySQLPool client) {
        this.client = client;
    }

    /**
     * Recupera todas las zonas asociadas a un usuario específico.
     *
     * @param usuarioId Identificador del usuario/basurero
     * @return Future con RowSet de filas que contienen:
     *         - id     (ID de la zona)
     *         - nombre (Nombre de la zona)
     *         - geom   (Geometría de la zona, puede ser JSON o texto)
     */
    public Future<RowSet<Row>> findZonasByUsuarioId(int usuarioId) {
        Promise<RowSet<Row>> promise = Promise.promise();
        String sql =
            "SELECT z.id, z.nombre, z.geom " +
            "FROM usuario_zona uz " +
            "JOIN zona z ON uz.id_zona = z.id " +
            "WHERE uz.id_usuario = ?";

        client.preparedQuery(sql)
              .execute(Tuple.of(usuarioId), ar -> {
                  if (ar.succeeded()) {
                      promise.complete(ar.result());
                  } else {
                      promise.fail(ar.cause());
                  }
              });

        return promise.future();
    }
}
