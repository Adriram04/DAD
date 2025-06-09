// MqttVerticle.java
package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.core.json.JsonObject;
import com.hackforchange.reciclaje_backend.repository.MqttRepository;
import com.hackforchange.reciclaje_backend.service.MqttService;
import com.hackforchange.reciclaje_backend.database.MySQLClientProvider;

public class MqttVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject config = config();
        String host = config.getJsonObject("mqtt").getString("host", "localhost");
        int    port = config.getJsonObject("mqtt").getInteger("port", 1883);

        MySQLPool client = MySQLClientProvider.createMySQLPool(vertx, config);
        MqttClient mqtt = MqttClient.create(vertx, new MqttClientOptions());

        mqtt.connect(port, host, ar -> {
            if (ar.succeeded()) {
                System.out.println("✅ MQTT conectado a " + host + ":" + port);
                var service = new MqttService(mqtt, new MqttRepository(client));
                service.subscribeTopics();
                startPromise.complete();
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }
}
