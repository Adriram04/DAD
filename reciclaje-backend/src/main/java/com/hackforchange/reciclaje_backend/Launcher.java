package com.hackforchange.reciclaje_backend;

import com.hackforchange.reciclaje_backend.config.*;
import io.vertx.core.Vertx;
import io.vertx.core.DeploymentOptions;

public class Launcher {
    public static void main(String[] args) {
        // Deshabilitar el DnsResolver para evitar errores en Azure
        System.setProperty("vertx.disableDnsResolver", "true");

        Vertx vertx = Vertx.vertx();

        ConfigLoader.load(vertx).onSuccess(config -> {
            System.out.println("✅ Config cargada correctamente:");
            System.out.println(config.encodePrettily());

            DeploymentOptions options = new DeploymentOptions().setConfig(config);
            vertx.deployVerticle(new MainApp(), options)
                .onSuccess(id -> System.out.println("🟢 MainApp desplegado con ID: " + id))
                .onFailure(err -> System.err.println("❌ Fallo al desplegar MainApp: " + err.getMessage()));

            System.out.println("🟡 Después de intentar desplegar MainApp...");
        }).onFailure(err -> {
            System.err.println("❌ Error al cargar configuración: " + err.getMessage());
        });
    }
}
