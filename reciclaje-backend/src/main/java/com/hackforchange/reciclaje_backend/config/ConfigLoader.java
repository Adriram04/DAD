package com.hackforchange.reciclaje_backend.config;

import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class ConfigLoader {

    public static Future<JsonObject> load(Vertx vertx) {
        Promise<JsonObject> promise = Promise.promise();

        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream("config.json")) {
            if (is == null) {
                promise.fail(new RuntimeException("No se encontró config.json en el classpath"));
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                JsonObject config = new JsonObject(content);
                promise.complete(config);
            }
        } catch (Exception e) {
            promise.fail(e);
        }
        
        return promise.future();
    }
}
