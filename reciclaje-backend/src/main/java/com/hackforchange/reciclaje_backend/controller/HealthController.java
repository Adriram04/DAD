package com.hackforchange.reciclaje_backend.controller;

import io.vertx.ext.web.Router;

/**
 * HealthController expone un endpoint /health para verificar
 * si el backend está funcionando (sin comprobar la DB).
 */
public class HealthController {

    public HealthController() {
        // No necesitamos nada más aquí
    }

    /**
     * Registra la ruta GET /health en el router proporcionado.
     * Devuelve un status 200 y un texto simple si todo está funcionando.
     */
    public void getRouter(Router router) {
        router.get("/health").handler(ctx -> {
            ctx.response()
               .setStatusCode(200)
               .end("Backend is up!");
        });
    }
}
