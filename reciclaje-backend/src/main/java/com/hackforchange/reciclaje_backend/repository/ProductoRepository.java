// ProductoRepository.java
package com.hackforchange.reciclaje_backend.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.mysqlclient.MySQLPool;

public class ProductoRepository {
    private final MySQLPool client;

    public ProductoRepository(MySQLPool client) {
        this.client = client;
    }

    public Future<RowSet<Row>> findAll() {
        Promise<RowSet<Row>> p = Promise.promise();
        String sql = "SELECT id, nombre, puntos_necesarios, id_proveedor FROM producto";
        client.query(sql).execute(ar -> {
            if (ar.succeeded()) p.complete(ar.result());
            else                p.fail(ar.cause());
        });
        return p.future();
    }

    public Future<Row> findById(int id) {
        Promise<Row> p = Promise.promise();
        String sql = "SELECT id, nombre, puntos_necesarios, id_proveedor FROM producto WHERE id = ?";
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

    public Future<Void> insert(String nombre, int puntos, int idProveedor) {
        Promise<Void> p = Promise.promise();
        String sql = "INSERT INTO producto (nombre, puntos_necesarios, id_proveedor) VALUES (?,?,?)";
        client.preparedQuery(sql).execute(Tuple.of(nombre, puntos, idProveedor), ar -> {
            if (ar.succeeded()) p.complete();
            else                p.fail(ar.cause());
        });
        return p.future();
    }

    public Future<Integer> update(int id, String nombre, int puntos, int idProveedor) {
        Promise<Integer> p = Promise.promise();
        String sql = "UPDATE producto SET nombre = ?, puntos_necesarios = ?, id_proveedor = ? WHERE id = ?";
        client.preparedQuery(sql).execute(Tuple.of(nombre, puntos, idProveedor, id), ar -> {
            if (ar.succeeded()) p.complete(ar.result().rowCount());
            else                p.fail(ar.cause());
        });
        return p.future();
    }

    public Future<Integer> delete(int id) {
        Promise<Integer> p = Promise.promise();
        String sql = "DELETE FROM producto WHERE id = ?";
        client.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) p.complete(ar.result().rowCount());
            else                p.fail(ar.cause());
        });
        return p.future();
    }
}
