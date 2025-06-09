// TarjetaController.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import com.hackforchange.reciclaje_backend.repository.TarjetaRepository;
import com.hackforchange.reciclaje_backend.service.TarjetaService;
import io.vertx.mysqlclient.MySQLPool;

public class TarjetaController {

    private final TarjetaService service;

    public TarjetaController(MySQLPool client) {
        this.service = new TarjetaService(new TarjetaRepository(client));
    }

    public void getRouter(Router router) {
        router.post("/tarjetas")
              .handler(BodyHandler.create())
              .handler(this::handleCreate);
    }

    private void handleCreate(RoutingContext ctx) {
        JsonObject b = ctx.getBodyAsJson();
        String uid = b.getString("uid");
        Integer consumidorId = b.getInteger("id_consumidor");

        service.createTarjeta(uid, consumidorId)
            .onSuccess(v -> ctx.response().setStatusCode(201).end())
            .onFailure(err -> {
                String msg = err.getMessage();
                if (msg.contains("obligatorios")) {
                    ctx.response().setStatusCode(400).end("❌ " + msg);
                } else {
                    ctx.fail(500, err);
                }
            });
    }
}
