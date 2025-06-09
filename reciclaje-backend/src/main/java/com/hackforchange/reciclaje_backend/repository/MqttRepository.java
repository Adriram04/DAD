package com.hackforchange.reciclaje_backend.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class MqttRepository {
    private final MySQLPool client;

    public MqttRepository(MySQLPool client) {
        this.client = client;
    }

    public Future<Row> fetchContenedor(int contenedorId) {
        Promise<Row> p = Promise.promise();
        String sql = "SELECT capacidad_maxima, carga_actual FROM contenedor WHERE id = ? LIMIT 1";
        client.preparedQuery(sql).execute(Tuple.of(contenedorId), ar -> {
            if (ar.failed() || ar.result().size() == 0) {
                p.complete(null); // no existe o falla
            } else {
                p.complete(ar.result().iterator().next());
            }
        });
        return p.future();
    }

    public Future<Void> updateTemperaturaGlobal(boolean bloquear) {
        Promise<Void> p = Promise.promise();
        String sql = "UPDATE contenedor SET bloqueo = ?";
        client.preparedQuery(sql)
              .execute(Tuple.of(bloquear), ar -> {
                  if (ar.succeeded()) p.complete();
                  else                p.fail(ar.cause());
              });
        return p.future();
    }

    public Future<Void> processReciclaje(int idConsumidor, int idContenedor, String qr,
                                         String tipoBasura, float kg, int puntos,
                                         float nuevaCarga, boolean lleno, boolean bloqueo) {
        return client.withTransaction(tx ->
            tx.preparedQuery(
                "INSERT INTO registro_reciclaje " +
                "(id_consumidor,id_contenedor,qr,tipo_basura,peso_kg,puntos_obtenidos) " +
                "VALUES (?,?,?,?,?,?)")
              .execute(Tuple.of(idConsumidor, idContenedor, qr, tipoBasura, kg, puntos))
            .compose(v -> tx.preparedQuery(
                "UPDATE contenedor SET carga_actual=?, lleno=?, bloqueo=? WHERE id=?")
              .execute(Tuple.of(nuevaCarga, lleno, bloqueo, idContenedor)))
            .compose(v -> tx.preparedQuery(
                "UPDATE usuario SET puntos = puntos + ? WHERE id=?")
              .execute(Tuple.of(puntos, idConsumidor)))
        )
        // con .mapEmpty() convertimos el Future<RowSet<Row>> a Future<Void>
        .mapEmpty();
    }
}
