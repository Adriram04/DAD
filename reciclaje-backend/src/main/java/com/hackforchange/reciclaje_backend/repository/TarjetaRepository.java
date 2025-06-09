// TarjetaRepository.java
package com.hackforchange.reciclaje_backend.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.RowSet;
import io.vertx.mysqlclient.MySQLPool;

public class TarjetaRepository {
    private final MySQLPool client;

    public TarjetaRepository(MySQLPool client) {
        this.client = client;
    }

    public Future<Void> insertTarjeta(String uid, int consumidorId) {
        Promise<Void> p = Promise.promise();
        String sql = "INSERT INTO tarjeta(uid, id_consumidor) VALUES(?, ?)";
        client.preparedQuery(sql)
              .execute(Tuple.of(uid, consumidorId), ar -> {
                  if (ar.succeeded()) p.complete();
                  else                p.fail(ar.cause());
              });
        return p.future();
    }
}
