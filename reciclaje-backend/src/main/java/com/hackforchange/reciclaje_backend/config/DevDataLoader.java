package com.hackforchange.reciclaje_backend.config;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;

public class DevDataLoader {

    public static void loadInitialUsers(MySQLPool client) {
        client.query("SELECT COUNT(*) as total FROM usuario").execute(ar -> {
            if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                int total = result.iterator().next().getInteger("total");

                if (total == 0) {
                    System.out.println("🧪 No hay usuarios en la base de datos. Insertando datos de prueba...");

                    insertUser(client, "Admin Uno", "admin1", "admin@hackforchange.com", "admin123", "ADMINISTRADOR");
                    insertUser(client, "Juan Reciclador", "juan1", "juan@correo.com", "juan123", "CONSUMIDOR");
                    insertUser(client, "Tienda Eco", "tienda", "eco@tienda.com", "eco123", "PROVEEDOR");
                    insertUser(client, "María Recolectora", "maria", "maria@recolector.com", "maria123", "BASURERO");
                } else {
                    System.out.println("✅ Usuarios ya existen en la base de datos. No se insertaron datos.");
                }
            } else {
                System.err.println("❌ Error al verificar usuarios: " + ar.cause().getMessage());
            }
        });
    }

    private static void insertUser(MySQLPool client, String nombre, String usuario, String email, String password, String rol) {
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        String sql = "INSERT INTO usuario (nombre, usuario, email, password, rol) VALUES (?, ?, ?, ?, ?)";
        client.preparedQuery(sql).execute(Tuple.of(nombre, usuario, email, hashedPassword, rol), ar -> {
            if (ar.succeeded()) {
                System.out.println("✅ Usuario insertado: " + email);
            } else {
                System.err.println("❌ Error al insertar usuario " + email + ": " + ar.cause().getMessage());
            }
        });
    }
}
