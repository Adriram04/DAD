// WalletController.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import com.hackforchange.reciclaje_backend.repository.WalletRepository;
import com.hackforchange.reciclaje_backend.service.WalletService;
import com.hackforchange.reciclaje_backend.wallet.GoogleWalletService;
import io.vertx.mysqlclient.MySQLPool;

public class WalletController {

    private final WalletService service;

    public WalletController(MySQLPool client, GoogleWalletService wallet) {
        this.service = new WalletService(new WalletRepository(client), wallet);
    }

    public void getRouter(Router router) {
        router.get("/wallet/link").handler(this::handleLink);
    }

    private void handleLink(RoutingContext ctx) {
        String userParam = ctx.queryParam("userId").stream().findFirst().orElse(null);
        if (userParam == null) {
            ctx.response().setStatusCode(400).end("❌ Parámetro userId requerido");
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userParam);
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ userId inválido");
            return;
        }

        service.generateLink(userId)
            .onSuccess(url -> ctx.json(new JsonObject().put("url", url)))
            .onFailure(err -> {
                String msg = err.getMessage();
                if (msg.equals("Usuario no encontrado")) {
                    ctx.response().setStatusCode(404).end("❌ Usuario no encontrado");
                } else if (msg.equals("Necesitas tarjeta física")) {
                    ctx.response().setStatusCode(409).end(new JsonObject().put("error", msg).encode());
                } else {
                    ctx.response().setStatusCode(500).end("❌ " + msg);
                }
            });
    }
}
