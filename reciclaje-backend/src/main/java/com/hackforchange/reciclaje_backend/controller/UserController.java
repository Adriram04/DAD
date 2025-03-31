package com.hackforchange.reciclaje_backend.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class UserController {

    private final MySQLPool client;

    public UserController(MySQLPool client) {
        this.client = client;
    }

    public Router getRouter(Router router) {
        router.get("/usuarios").handler(this::handleListUsers);
        router.post("/usuarios").handler(this::handleCreateUser);
        router.put("/usuarios/:id").handler(this::handleUpdateUser);
        router.delete("/usuarios/:id").handler(this::handleDeleteUser);
        return router;
    }

    private void handleListUsers(RoutingContext ctx) {
        client.query("SELECT id, nombre, usuario, email, rol FROM usuario").execute(ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                JsonArray usuarios = new JsonArray();

                for (Row row : rows) {
                    usuarios.add(row.toJson());
                }

                JsonObject response = new JsonObject().put("usuarios", usuarios);
                ctx.response().putHeader("Content-Type", "application/json").end(response.encode());
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al obtener usuarios");
            }
        });
    }


    private void handleCreateUser(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String nombre = body.getString("nombre");
        String usuario = body.getString("usuario");
        String email = body.getString("email");
        String password = body.getString("password");
        String rol = body.getString("rol");

        if (nombre == null || usuario == null || email == null || password == null || rol == null) {
            ctx.response().setStatusCode(400).end("❌ Faltan campos obligatorios");
            return;
        }

        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String sql = "INSERT INTO usuario (nombre, usuario, email, password, rol) VALUES (?, ?, ?, ?, ?)";

        client.preparedQuery(sql).execute(Tuple.of(nombre, usuario, email, hashedPassword, rol), ar -> {
            if (ar.succeeded()) {
                ctx.response().setStatusCode(201).end("✅ Usuario creado");
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al crear usuario: " + ar.cause().getMessage());
            }
        });
    }

    private void handleUpdateUser(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject body = ctx.body().asJsonObject();
        String nombre = body.getString("nombre");
        String usuario = body.getString("usuario");
        String email = body.getString("email");
        String rol = body.getString("rol");

        String sql = "UPDATE usuario SET nombre = ?, usuario = ?, email = ?, rol = ? WHERE id = ?";
        client.preparedQuery(sql).execute(Tuple.of(nombre, usuario, email, rol, id), ar -> {
            if (ar.succeeded()) {
                ctx.response().end("✅ Usuario actualizado");
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al actualizar usuario");
            }
        });
    }


    private void handleDeleteUser(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        client.preparedQuery("DELETE FROM usuario WHERE id = ?").execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                ctx.response().end("✅ Usuario eliminado");
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al eliminar usuario");
            }
        });
    }
}
