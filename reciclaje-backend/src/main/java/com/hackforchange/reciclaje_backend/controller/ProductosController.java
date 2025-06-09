// ProductosController.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import com.hackforchange.reciclaje_backend.repository.ProductoRepository;
import com.hackforchange.reciclaje_backend.service.ProductoService;
import io.vertx.mysqlclient.MySQLPool;

public class ProductosController {

    private final ProductoService service;

    public ProductosController(MySQLPool client) {
        this.service = new ProductoService(new ProductoRepository(client));
    }

    public void getRouter(Router router) {
        router.get("/productos").handler(this::listAll);
        router.get("/productos/:id").handler(this::getById);
        router.post("/productos").handler(BodyHandler.create()).handler(this::create);
        router.put("/productos/:id").handler(BodyHandler.create()).handler(this::update);
        router.delete("/productos/:id").handler(this::delete);
    }

    private void listAll(RoutingContext ctx) {
        service.listAll()
            .onSuccess(res -> ctx.json(res))
            .onFailure(err -> ctx.response().setStatusCode(500).end("❌ " + err.getMessage()));
    }

    private void getById(RoutingContext ctx) {
        int id;
        try { id = Integer.parseInt(ctx.pathParam("id")); }
        catch (NumberFormatException e) { ctx.response().setStatusCode(400).end("❌ ID inválido"); return; }

        service.getById(id)
            .onSuccess(res -> ctx.json(res))
            .onFailure(err -> {
                if (err.getMessage().equals("Producto no encontrado"))
                    ctx.response().setStatusCode(404).end("❌ " + err.getMessage());
                else
                    ctx.response().setStatusCode(500).end("❌ " + err.getMessage());
            });
    }

    private void create(RoutingContext ctx) {
        JsonObject b = ctx.getBodyAsJson();
        service.create(
                b.getString("nombre"),
                b.getInteger("puntos_necesarios"),
                b.getInteger("id_proveedor")
            )
            .onSuccess(v -> ctx.response().setStatusCode(201).end("✅ Producto creado"))
            .onFailure(err -> {
                if (err.getMessage().equals("Faltan campos necesarios"))
                    ctx.response().setStatusCode(400).end("❌ " + err.getMessage());
                else
                    ctx.response().setStatusCode(500).end("❌ " + err.getMessage());
            });
    }

    private void update(RoutingContext ctx) {
        int id;
        try { id = Integer.parseInt(ctx.pathParam("id")); }
        catch (NumberFormatException e) { ctx.response().setStatusCode(400).end("❌ ID inválido"); return; }
        JsonObject b = ctx.getBodyAsJson();
        service.update(
                id,
                b.getString("nombre"),
                b.getInteger("puntos_necesarios"),
                b.getInteger("id_proveedor")
            )
            .onSuccess(v -> ctx.response().end("✅ Producto actualizado"))
            .onFailure(err -> {
                if (err.getMessage().equals("Faltan campos necesarios"))
                    ctx.response().setStatusCode(400).end("❌ " + err.getMessage());
                else if (err.getMessage().equals("Producto no encontrado"))
                    ctx.response().setStatusCode(404).end("❌ " + err.getMessage());
                else
                    ctx.response().setStatusCode(500).end("❌ " + err.getMessage());
            });
    }

    private void delete(RoutingContext ctx) {
        int id;
        try { id = Integer.parseInt(ctx.pathParam("id")); }
        catch (NumberFormatException e) { ctx.response().setStatusCode(400).end("❌ ID inválido"); return; }

        service.delete(id)
            .onSuccess(v -> ctx.response().end("✅ Producto eliminado"))
            .onFailure(err -> {
                if (err.getMessage().equals("Producto no encontrado"))
                    ctx.response().setStatusCode(404).end("❌ " + err.getMessage());
                else
                    ctx.response().setStatusCode(500).end("❌ " + err.getMessage());
            });
    }
}
