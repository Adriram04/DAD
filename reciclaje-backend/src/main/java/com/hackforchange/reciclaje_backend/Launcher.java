package com.hackforchange.reciclaje_backend;

import com.hackforchange.reciclaje_backend.config.*;
import io.vertx.core.Vertx;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;

public class Launcher {
    public static void main(String[] args) {
        // 👇 Esto evita que Vert.x toque el sistema DNS (archivos o interfaces de red)
        AddressResolverOptions resolverOptions = new AddressResolverOptions()
            .setOptResourceEnabled(false)  // Desactiva carga de /etc/resolv.conf y similares
            .setHostsPath(null)
            .setHostsValue(null);

        VertxOptions vertxOptions = new VertxOptions()
            .setAddressResolverOptions(resolverOptions);

        Vertx vertx = Vertx.vertx(vertxOptions);

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
