// UserController.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import com.hackforchange.reciclaje_backend.repository.UserRepository;
import com.hackforchange.reciclaje_backend.service.UserService;
import io.vertx.mysqlclient.MySQLPool;

public class UserController {

    private final UserService service;

    public UserController(MySQLPool client) {
        this.service = new UserService(new UserRepository(client));
    }

    public Router getRouter(Router router) {
        router.get("/usuarios").handler(this::listUsers);
        router.get("/usuarios/:id/perfil").handler(this::getPerfil);
        router.get("/usuarios/leaderboard").handler(this::getLeaderboard);
        router.post("/usuarios").handler(BodyHandler.create()).handler(this::createUser);
        router.put("/usuarios/:id").handler(BodyHandler.create()).handler(this::updateUser);
        router.delete("/usuarios/:id").handler(this::deleteUser);
        return router;
    }

    private void listUsers(RoutingContext ctx) {
        service.listUsers()
            .onSuccess(res -> ctx.json(res))
            .onFailure(err -> ctx.fail(500, err));
    }

    private void getPerfil(RoutingContext ctx) {
        int id; 
        try { id = Integer.parseInt(ctx.pathParam("id")); }
        catch (Exception e) { ctx.fail(400); return; }
        service.getPerfil(id)
            .onSuccess(res -> ctx.json(res))
            .onFailure(err -> ctx.fail(err.equals("Perfil no encontrado")?404:500, err));
    }

    private void getLeaderboard(RoutingContext ctx) {
        int limit = 50;
        if (!ctx.queryParam("limit").isEmpty()) {
            try { limit = Integer.parseInt(ctx.queryParam("limit").get(0)); }
            catch (NumberFormatException ignored) {}
        }
        service.getLeaderboard(limit)
            .onSuccess(res -> ctx.json(res))
            .onFailure(err -> ctx.fail(500, err));
    }

    private void createUser(RoutingContext ctx) {
        JsonObject b = ctx.getBodyAsJson();
        try {
            service.createUser(
                b.getString("nombre"),
                b.getString("usuario"),
                b.getString("email"),
                b.getString("password"),
                b.getString("rol")
            ).onSuccess(v -> ctx.response().setStatusCode(201).end("✅ Usuario creado"))
             .onFailure(err -> ctx.fail(500, err));
        } catch (Exception e) {
            ctx.fail(400);
        }
    }

    private void updateUser(RoutingContext ctx) {
        int id;
        try { id = Integer.parseInt(ctx.pathParam("id")); }
        catch (Exception e) { ctx.fail(400); return; }
        JsonObject b = ctx.getBodyAsJson();
        service.updateUser(
            id,
            b.getString("nombre"),
            b.getString("usuario"),
            b.getString("email"),
            b.getString("rol")
        ).onSuccess(v -> ctx.response().end("✅ Usuario actualizado"))
         .onFailure(err -> ctx.fail(err.equals("Usuario no encontrado")?404:500, err));
    }

    private void deleteUser(RoutingContext ctx) {
        int id;
        try { id = Integer.parseInt(ctx.pathParam("id")); }
        catch (Exception e) { ctx.fail(400); return; }
        service.deleteUser(id)
            .onSuccess(v -> ctx.response().end("✅ Usuario eliminado"))
            .onFailure(err -> ctx.fail(err.equals("Usuario no encontrado")?404:500, err));
    }
}
