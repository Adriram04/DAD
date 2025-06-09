// AuthService.java
package com.hackforchange.reciclaje_backend.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import com.hackforchange.reciclaje_backend.repository.AuthRepository;
import com.hackforchange.reciclaje_backend.security.JwtProvider;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;

/**
 * Servicio que implementa la lógica de negocio de autenticación:
 * registro de usuarios y generación/validación de credenciales.
 * Utiliza {@link AuthRepository} para operaciones sobre la tabla usuario
 * y {@link JwtProvider} para emitir tokens JWT.
 */
public class AuthService {

    /** Repositorio para acceso a datos de usuarios */
    private final AuthRepository repo;

    /** Proveedor de tokens JWT */
    private final JwtProvider jwt;

    /**
     * Constructor.
     *
     * @param repo  Repositorio de autenticación
     * @param vertx Contexto de Vert.x necesario para JwtProvider
     */
    public AuthService(AuthRepository repo, Vertx vertx) {
        this.repo = repo;
        this.jwt  = new JwtProvider(vertx);
    }

    /**
     * Registra un nuevo usuario en la base de datos.
     * Hashea la contraseña con BCrypt antes de insertar.
     *
     * @param nombre   Nombre completo del usuario
     * @param usuario  Nombre de usuario (username)
     * @param email    Correo electrónico
     * @param password Contraseña en texto plano
     * @param rol      Rol asignado (p.ej. CONSUMIDOR, ADMIN)
     * @return Future completado cuando la inserción en BD finaliza
     */
    public Future<Void> register(String nombre, String usuario, String email,
                                 String password, String rol) {
        // Generar hash seguro de la contraseña
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        // Delegar en el repositorio
        return repo.insertUser(nombre, usuario, email, hash, rol);
    }

    /**
     * Autentica un usuario y, de ser exitoso, emite un token JWT junto con
     * los datos del usuario (sin contraseña).
     *
     * @param email    Correo electrónico registrado
     * @param password Contraseña en texto plano
     * @return Future completado con JsonObject:
     *         { "token": "...", "user": { id, nombre, usuario, email, rol, puntos } }
     */
    public Future<JsonObject> login(String email, String password) {
        Promise<JsonObject> promise = Promise.promise();

        // Buscar usuario por email
        repo.findUserByEmail(email)
            .onSuccess(row -> {
                // Si no existe, falla
                if (row == null) {
                    promise.fail("Usuario no encontrado");
                    return;
                }

                // Verificar contraseña contra el hash almacenado
                String hash = row.getString("password");
                boolean verified = BCrypt.verifyer()
                                         .verify(password.toCharArray(), hash)
                                         .verified;
                if (!verified) {
                    promise.fail("Contraseña incorrecta");
                } else {
                    // Construir payload de JWT (sin incluir password)
                    JsonObject payload = new JsonObject()
                        .put("id",    row.getInteger("id"))
                        .put("email", row.getString("email"))
                        .put("rol",   row.getString("rol"));

                    // Generar token
                    String token = jwt.generateToken(payload);

                    // Preparar datos de usuario para la respuesta
                    JsonObject userOut = new JsonObject()
                        .put("id",      row.getInteger("id"))
                        .put("nombre",  row.getString("nombre"))
                        .put("usuario", row.getString("usuario"))
                        .put("email",   row.getString("email"))
                        .put("rol",     row.getString("rol"))
                        .put("puntos",  row.getInteger("puntos"));

                    // Respuesta final con token y usuario
                    JsonObject result = new JsonObject()
                        .put("token", token)
                        .put("user",  userOut);

                    promise.complete(result);
                }
            })
            .onFailure(promise::fail);

        return promise.future();
    }
}
