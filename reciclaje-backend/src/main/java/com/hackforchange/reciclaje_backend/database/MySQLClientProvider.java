package com.hackforchange.reciclaje_backend.database;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.mysqlclient.SslMode;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;  // Importamos PemTrustOptions para usarlo con certificados de confianza
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class MySQLClientProvider {

    public static MySQLPool createMySQLPool(Vertx vertx, JsonObject config) {
        JsonObject dbConfig = config.getJsonObject("db");

        // Cargar el archivo del certificado desde resources
        String certPath = getPemCertificate();  // Ruta del certificado en resources

        // Leemos el certificado para establecer la conexión segura
        Buffer certBuffer;
        try {
            certBuffer = Buffer.buffer(Files.readAllBytes(Paths.get(certPath)));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar el certificado: " + e.getMessage(), e);
        }

        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                .setPort(dbConfig.getInteger("port"))
                .setHost(dbConfig.getString("host"))
                .setDatabase(dbConfig.getString("database"))
                .setUser(dbConfig.getString("user"))
                .setPassword(dbConfig.getString("password"))
                .setSsl(true)  // Activamos SSL
                .setTrustAll(false) // Desactivamos la aceptación de todos los certificados
                .setSslMode(SslMode.REQUIRED)  // Usamos el modo requerido para SSL
                .setPemTrustOptions(new PemTrustOptions().addCertValue(certBuffer)); // Usamos el certificado PEM de la CA para la conexión

        PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

        return MySQLPool.pool(vertx, connectOptions, poolOptions);
    }

    // Método para cargar el archivo .pem desde resources
    private static String getPemCertificate() {
        // Usamos el ClassLoader para obtener la ruta del archivo en resources
        String certPath = MySQLClientProvider.class.getClassLoader().getResource("DigiCertGlobalRootG2.crt.pem").getFile();
        if (certPath == null) {
            throw new RuntimeException("El certificado no se encuentra en la ruta esperada.");
        }
        return certPath;
    }
}
