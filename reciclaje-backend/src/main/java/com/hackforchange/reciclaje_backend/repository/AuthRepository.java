// AuthRepository.java
package com.hackforchange.reciclaje_backend.repository;

import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.Future;
import io.vertx.core.Promise;

/**
 * Repositorio encargado de las operaciones de persistencia para la entidad Usuario.
 * Proporciona métodos para insertar nuevos usuarios y recuperar usuarios por correo.
 */
public class AuthRepository {

    /** Pool de conexiones MySQL proporcionado por el verticle principal */
    private final MySQLPool client;

    /**
     * Constructor.
     *
     * @param client Pool de conexiones MySQL para ejecutar queries
     */
    public AuthRepository(MySQLPool client) {
        this.client = client;
    }

    /**
     * Inserta un nuevo usuario en la base de datos.
     *
     * @param nombre         Nombre completo del usuario
     * @param usuario        Nombre de usuario (username)
     * @param email          Correo electrónico único
     * @param hashedPassword Contraseña hasheada con BCrypt
     * @param rol            Rol de usuario (p.ej. ADMIN, CONSUMIDOR)
     * @return Future que se completa cuando la inserción finaliza, o falla con la causa del error
     */
    public Future<Void> insertUser(String nombre,
                                   String usuario,
                                   String email,
                                   String hashedPassword,
                                   String rol) {
        Promise<Void> promise = Promise.promise();
        String sql = "INSERT INTO usuario (nombre, usuario, email, password, rol) VALUES (?,?,?,?,?)";
        client.preparedQuery(sql)
              .execute(Tuple.of(nombre, usuario, email, hashedPassword, rol), ar -> {
                  if (ar.succeeded()) promise.complete();
                  else                promise.fail(ar.cause());
              });
        return promise.future();
    }

    /**
     * Recupera un usuario por su correo electrónico.
     *
     * @param email Correo electrónico del usuario a buscar
     * @return Future con la primera fila encontrada (fila con columnas:
     *         id, nombre, usuario, email, password, rol, puntos), o null si no existe,
     *         o falla con la causa del error.
     */
    public Future<Row> findUserByEmail(String email) {
        Promise<Row> promise = Promise.promise();
        String sql = "SELECT id, nombre, usuario, email, password, rol, puntos "
                   + "FROM usuario WHERE email = ? LIMIT 1";
        client.preparedQuery(sql)
              .execute(Tuple.of(email), ar -> {
                  if (ar.failed()) {
                      promise.fail(ar.cause());
                  } else {
                      RowSet<Row> rows = ar.result();
                      // Si no hay filas, retornamos null para indicar "no encontrado"
                      promise.complete(rows.iterator().hasNext() ? rows.iterator().next() : null);
                  }
              });
        return promise.future();
    }
}
