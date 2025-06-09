// UserRepository.java
package com.hackforchange.reciclaje_backend.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.mysqlclient.MySQLPool;

public class UserRepository {
    private final MySQLPool client;

    public UserRepository(MySQLPool client) {
        this.client = client;
    }

    public Future<RowSet<Row>> findAll() {
        Promise<RowSet<Row>> p = Promise.promise();
        client.query("SELECT id,nombre,usuario,email,rol,puntos FROM usuario")
              .execute(ar -> {
                  if (ar.succeeded()) p.complete(ar.result());
                  else                p.fail(ar.cause());
              });
        return p.future();
    }

    public Future<Row> findPerfil(int id) {
        Promise<Row> p = Promise.promise();
        String sql =
            "SELECT u.id,u.nombre,u.email,u.rol,u.puntos, t.uid AS card_uid " +
            "FROM usuario u LEFT JOIN tarjeta t ON t.id_consumidor=u.id " +
            "WHERE u.id=? LIMIT 1";
        client.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.failed()) {
                p.fail(ar.cause());
            } else {
                RowSet<Row> rs = ar.result();
                if (!rs.iterator().hasNext()) p.complete();
                else                          p.complete(rs.iterator().next());
            }
        });
        return p.future();
    }

    public Future<RowSet<Row>> findLeaderboard(int limit) {
        Promise<RowSet<Row>> p = Promise.promise();
        String sql =
            "SELECT id,nombre,puntos FROM usuario " +
            "WHERE rol='CONSUMIDOR' ORDER BY puntos DESC LIMIT ?";
        client.preparedQuery(sql).execute(Tuple.of(limit), ar -> {
            if (ar.succeeded()) p.complete(ar.result());
            else                p.fail(ar.cause());
        });
        return p.future();
    }

    public Future<Void> insert(String nombre, String usuario, String email, String hash, String rol) {
        Promise<Void> p = Promise.promise();
        String sql = "INSERT INTO usuario(nombre,usuario,email,password,rol) VALUES(?,?,?,?,?)";
        client.preparedQuery(sql).execute(
            Tuple.of(nombre, usuario, email, hash, rol),
            ar -> {
                if (ar.succeeded()) p.complete();
                else                p.fail(ar.cause());
            });
        return p.future();
    }

    public Future<Integer> update(int id, String nombre, String usuario, String email, String rol) {
        Promise<Integer> p = Promise.promise();
        String sql = "UPDATE usuario SET nombre=?,usuario=?,email=?,rol=? WHERE id=?";
        client.preparedQuery(sql).execute(
            Tuple.of(nombre, usuario, email, rol, id),
            ar -> {
                if (ar.succeeded()) p.complete(ar.result().rowCount());
                else                p.fail(ar.cause());
            });
        return p.future();
    }

    public Future<Integer> delete(int id) {
        Promise<Integer> p = Promise.promise();
        client.preparedQuery("DELETE FROM usuario WHERE id=?")
              .execute(Tuple.of(id), ar -> {
                  if (ar.succeeded()) p.complete(ar.result().rowCount());
                  else                p.fail(ar.cause());
              });
        return p.future();
    }
}
