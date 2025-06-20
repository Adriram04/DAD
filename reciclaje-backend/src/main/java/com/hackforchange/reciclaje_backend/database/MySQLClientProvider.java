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

        // 1) Obtenemos InputStream del PEM (desde el classpath/jar)
        InputStream certStream = MySQLClientProvider.class.getClassLoader()
                .getResourceAsStream("DigiCertGlobalRootCA.crt.pem");
        if (certStream == null) {
            throw new RuntimeException("❌ No se pudo encontrar DigiCertGlobalRootCA.crt.pem en el classpath");
        }

        // 2) Leemos todos los bytes del InputStream manualmente (compatibilidad con Java 8)
        Buffer certBuffer;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = certStream.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            certBuffer = Buffer.buffer(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("❌ Error leyendo el certificado PEM: " + e.getMessage(), e);
        } finally {
            try {
                certStream.close(); // Cerramos el stream
            } catch (IOException ignored) {}
        }

        // 3) Configuramos PemTrustOptions con ese Buffer
        PemTrustOptions pemTrustOptions = new PemTrustOptions().addCertValue(certBuffer);

        // 4) Creamos las opciones de conexión SSL
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
        return MySQLPool.pool(vertx, connectOptions, poolOptions);
    }
}
