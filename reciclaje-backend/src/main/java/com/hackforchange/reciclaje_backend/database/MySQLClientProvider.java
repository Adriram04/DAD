package com.hackforchange.reciclaje_backend.database;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.mysqlclient.SslMode;
import io.vertx.sqlclient.PoolOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MySQLClientProvider {

    public static MySQLPool createMySQLPool(Vertx vertx, JsonObject config) {
        JsonObject dbConfig = config.getJsonObject("db");

        // 1) Usamos executeBlocking para leer el archivo PEM de forma asíncrona
        vertx.executeBlocking(promise -> {
            InputStream certStream = MySQLClientProvider.class.getClassLoader()
                    .getResourceAsStream("DigiCertGlobalRootG2.crt.pem");
            if (certStream == null) {
                promise.fail("❌ No se pudo encontrar DigiCertGlobalRootG2.crt.pem en el classpath");
                return;
            }

            // 2) Leemos todos los bytes del InputStream manualmente (compatibilidad con Java 8)
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = certStream.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                Buffer certBuffer = Buffer.buffer(bos.toByteArray());
                promise.complete(certBuffer);  // Devuelve el Buffer al completar
            } catch (IOException e) {
                promise.fail("❌ Error leyendo el certificado PEM: " + e.getMessage());
            } finally {
                try {
                    certStream.close(); // Cerramos el stream
                } catch (IOException ignored) {}
            }
        }, res -> {
            if (res.succeeded()) {
                // 3) Si la lectura del certificado fue exitosa, continuamos con la configuración de la base de datos
                Buffer certBuffer = (Buffer) res.result();
                PemTrustOptions pemTrustOptions = new PemTrustOptions().addCertValue(certBuffer);

                // 4) Configuramos las opciones de conexión a MySQL
                MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                        .setPort(dbConfig.getInteger("port"))
                        .setHost(dbConfig.getString("host"))
                        .setDatabase(dbConfig.getString("database"))
                        .setUser(dbConfig.getString("user"))
                        .setPassword(dbConfig.getString("password"))
                        .setSsl(true)
                        .setTrustAll(true)
                        .setSslMode(SslMode.REQUIRED)
                        .setPemTrustOptions(pemTrustOptions);

                PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

                // 5) Retornamos el pool de conexiones
                MySQLPool pool = MySQLPool.pool(vertx, connectOptions, poolOptions);
            } else {
                // 6) Si hubo un error en la lectura del certificado, manejamos el fallo
                System.err.println(res.cause().getMessage());
            }
        });

        // En este punto la función no retorna inmediatamente, porque la creación del pool depende de la lectura del archivo PEM
        return null; // Este valor debería ser retornado después de que se complete el executeBlocking
    }
}
