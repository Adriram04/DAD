// UserService.java
package com.hackforchange.reciclaje_backend.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import com.hackforchange.reciclaje_backend.repository.UserRepository;

public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public Future<JsonObject> listUsers() {
        Promise<JsonObject> p = Promise.promise();
        repo.findAll().onSuccess(rows -> {
            JsonArray arr = new JsonArray();
            rows.forEach(r -> arr.add(r.toJson()));
            p.complete(new JsonObject().put("usuarios", arr));
        }).onFailure(p::fail);
        return p.future();
    }

    public Future<JsonObject> getPerfil(int id) {
        Promise<JsonObject> p = Promise.promise();
        repo.findPerfil(id).onSuccess(row -> {
            if (row == null) p.fail("Perfil no encontrado");
            else            p.complete(row.toJson());
        }).onFailure(p::fail);
        return p.future();
    }

    public Future<JsonObject> getLeaderboard(int limit) {
        Promise<JsonObject> p = Promise.promise();
        repo.findLeaderboard(limit).onSuccess(rows -> {
            JsonArray arr = new JsonArray();
            for (Row r : rows) {
                arr.add(new JsonObject()
                    .put("id",     r.getInteger("id"))
                    .put("nombre", r.getString("nombre"))
                    .put("puntos",r.getInteger("puntos")));
            }
            p.complete(new JsonObject().put("usuarios", arr));
        }).onFailure(p::fail);
        return p.future();
    }

    public Future<Void> createUser(String nombre, String usuario, String email, String password, String rol) {
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        return repo.insert(nombre, usuario, email, hash, rol);
    }

    public Future<Void> updateUser(int id, String nombre, String usuario, String email, String rol) {
        return repo.update(id, nombre, usuario, email, rol).compose(rows -> {
            if (rows == 0) return Future.failedFuture("Usuario no encontrado");
            else           return Future.succeededFuture();
        });
    }

    public Future<Void> deleteUser(int id) {
        return repo.delete(id).compose(rows -> {
            if (rows == 0) return Future.failedFuture("Usuario no encontrado");
            else           return Future.succeededFuture();
        });
    }
}
