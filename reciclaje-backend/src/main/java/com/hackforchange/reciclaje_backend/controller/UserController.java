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
        router.get("/usuarios/:id/perfil").handler(this::handleGetPerfil);
        router.get("/usuarios/leaderboard").handler(this::handleLeaderboard);   // 🆕
        router.post("/usuarios").handler(this::handleCreateUser);
        router.put("/usuarios/:id").handler(this::handleUpdateUser);
        router.delete("/usuarios/:id").handler(this::handleDeleteUser);
        return router;
    }

    /* ---------- GET /usuarios ---------- */
    private void handleListUsers(RoutingContext ctx) {
        client.query("SELECT id,nombre,usuario,email,rol,puntos FROM usuario").execute(ar -> {
            if (ar.succeeded()) {
                JsonArray usuarios = new JsonArray();
                ar.result().forEach(row -> usuarios.add(row.toJson()));
                ctx.json(new JsonObject().put("usuarios", usuarios));
            } else ctx.fail(ar.cause());
        });
    }

    /* ---------- GET /usuarios/:id/perfil ---------- */
    private void handleGetPerfil(RoutingContext ctx) {
        int id;
        try { id = Integer.parseInt(ctx.pathParam("id")); }
        catch (NumberFormatException e) { ctx.fail(400); return; }

        String sql =
          "SELECT u.id,u.nombre,u.email,u.rol,u.puntos, t.uid AS card_uid " +
          "FROM usuario u LEFT JOIN tarjeta t ON t.id_consumidor=u.id " +
          "WHERE u.id=? LIMIT 1";

        client.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.succeeded() && ar.result().size() > 0)
                ctx.json(ar.result().iterator().next().toJson());
            else if (ar.succeeded())
                ctx.fail(404);
            else ctx.fail(ar.cause());
        });
    }

    /* ---------- GET /usuarios/leaderboard ---------- */
    private void handleLeaderboard(RoutingContext ctx) {
        // Podemos recibir ?limit=n, por defecto 50
        int limit = 50;
        if (!ctx.queryParam("limit").isEmpty()) {
            try {
                limit = Integer.parseInt(ctx.queryParam("limit").get(0));
            } catch (NumberFormatException e) {
                // si no es numérico, seguimos con 50
            }
        }

        String sql = 
            "SELECT id, nombre, puntos "
          + "FROM usuario "
          + "WHERE rol = 'CONSUMIDOR' "
          + "ORDER BY puntos DESC "
          + "LIMIT ?";

        client.preparedQuery(sql).execute(Tuple.of(limit), ar -> {
            if (ar.succeeded()) {
                JsonArray arr = new JsonArray();
                for (Row row : ar.result()) {
                    // Solo necesitamos id, nombre y puntos en la EcoLiga
                    arr.add(new JsonObject()
                        .put("id", row.getInteger("id"))
                        .put("nombre", row.getString("nombre"))
                        .put("puntos", row.getInteger("puntos")));
                }
                ctx.json(new JsonObject().put("usuarios", arr));
            } else {
                ctx.fail(ar.cause());
            }
        });
    }

    /* ---------- POST /usuarios ---------- */
    private void handleCreateUser(RoutingContext ctx) {
        JsonObject b = ctx.body().asJsonObject();
        String nombre = b.getString("nombre");
        String usuario= b.getString("usuario");
        String email  = b.getString("email");
        String password=b.getString("password");
        String rol    = b.getString("rol");

        if (nombre==null||usuario==null||email==null||password==null||rol==null) {
            ctx.fail(400); return;
        }

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String sql  = "INSERT INTO usuario(nombre,usuario,email,password,rol) VALUES(?,?,?,?,?)";

        client.preparedQuery(sql).execute(Tuple.of(nombre,usuario,email,hash,rol), ar -> {
            if (ar.succeeded()) ctx.response().setStatusCode(201).end("✅ Usuario creado");
            else ctx.fail(ar.cause());
        });
    }

    /* ---------- PUT /usuarios/:id ---------- */
    private void handleUpdateUser(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject b = ctx.body().asJsonObject();
        String sql = "UPDATE usuario SET nombre=?,usuario=?,email=?,rol=? WHERE id=?";
        client.preparedQuery(sql).execute(
            Tuple.of(b.getString("nombre"), b.getString("usuario"),
                     b.getString("email"),  b.getString("rol"), id),
            ar -> { if (ar.succeeded()) ctx.end("✅ Usuario actualizado");
                    else ctx.fail(ar.cause()); });
    }

    /* ---------- DELETE /usuarios/:id ---------- */
    private void handleDeleteUser(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        client.preparedQuery("DELETE FROM usuario WHERE id=?").execute(
            Tuple.of(id), ar -> { if (ar.succeeded()) ctx.end("✅ Usuario eliminado");
                                  else ctx.fail(ar.cause()); });
    }
}
