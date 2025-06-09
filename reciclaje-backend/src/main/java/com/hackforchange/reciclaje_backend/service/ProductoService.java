// ProductoService.java
package com.hackforchange.reciclaje_backend.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import com.hackforchange.reciclaje_backend.repository.ProductoRepository;

public class ProductoService {
    private final ProductoRepository repo;

    public ProductoService(ProductoRepository repo) {
        this.repo = repo;
    }

    public Future<JsonObject> listAll() {
        Promise<JsonObject> p = Promise.promise();
        repo.findAll().onSuccess(rows -> {
            JsonArray arr = new JsonArray();
            for (Row r : rows) {
                arr.add(new JsonObject()
                    .put("id",              r.getInteger("id"))
                    .put("nombre",          r.getString("nombre"))
                    .put("puntos_necesarios", r.getInteger("puntos_necesarios"))
                    .put("id_proveedor",    r.getInteger("id_proveedor")));
            }
            p.complete(new JsonObject().put("productos", arr));
        }).onFailure(p::fail);
        return p.future();
    }

    public Future<JsonObject> getById(int id) {
        Promise<JsonObject> p = Promise.promise();
        repo.findById(id).onSuccess(row -> {
            if (row == null) {
                p.fail("Producto no encontrado");
            } else {
                p.complete(new JsonObject()
                    .put("id",               row.getInteger("id"))
                    .put("nombre",           row.getString("nombre"))
                    .put("puntos_necesarios", row.getInteger("puntos_necesarios"))
                    .put("id_proveedor",     row.getInteger("id_proveedor")));
            }
        }).onFailure(p::fail);
        return p.future();
    }

    public Future<Void> create(String nombre, Integer puntos, Integer idProveedor) {
        if (nombre == null || puntos == null || idProveedor == null) {
            return Future.failedFuture("Faltan campos necesarios");
        }
        return repo.insert(nombre, puntos, idProveedor);
    }

    public Future<Void> update(int id, String nombre, Integer puntos, Integer idProveedor) {
        if (nombre == null || puntos == null || idProveedor == null) {
            return Future.failedFuture("Faltan campos necesarios");
        }
        return repo.update(id, nombre, puntos, idProveedor).compose(rows -> {
            if (rows == 0) return Future.failedFuture("Producto no encontrado");
            else           return Future.succeededFuture();
        });
    }

    public Future<Void> delete(int id) {
        return repo.delete(id).compose(rows -> {
            if (rows == 0) return Future.failedFuture("Producto no encontrado");
            else           return Future.succeededFuture();
        });
    }
}
