// WalletRepository.java
package com.hackforchange.reciclaje_backend.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class WalletRepository {
    private final MySQLPool client;

    public WalletRepository(MySQLPool client) {
        this.client = client;
    }

    public Future<Row> findUserAndUid(int userId) {
        Promise<Row> p = Promise.promise();
        String sql = ""
          + "SELECT u.nombre, t.uid "
          + "FROM usuario u "
          + "LEFT JOIN tarjeta t ON t.id_consumidor = u.id "
          + "WHERE u.id = ? LIMIT 1";
        client.preparedQuery(sql).execute(Tuple.of(userId), ar -> {
            if (ar.failed()) {
                p.fail(ar.cause());
            } else {
                RowSet<Row> rs = ar.result();
                if (!rs.iterator().hasNext()) {
                    p.complete(); // null
                } else {
                    p.complete(rs.iterator().next());
                }
            }
        });
        return p.future();
    }
}
