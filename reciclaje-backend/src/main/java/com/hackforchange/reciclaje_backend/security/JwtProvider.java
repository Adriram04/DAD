package com.hackforchange.reciclaje_backend.security;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class JwtProvider {

    private final JWTAuth jwtAuth;

    public JwtProvider(Vertx vertx) {
        System.out.println("🔐 Inicializando JwtProvider...");

        try {
            // Construir solo la clave simétrica
            PubSecKeyOptions keyOptions = new PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setSymmetric(true)
                .setBuffer("super-secret-key");

            JWTAuthOptions authOptions = new JWTAuthOptions().addPubSecKey(keyOptions);

            System.out.println("🔑 Clave simétrica y algoritmo configurados (HS256).");

            this.jwtAuth = JWTAuth.create(vertx, authOptions);
            System.out.println("✅ JwtProvider inicializado correctamente.");
        } catch (Exception e) {
            System.err.println("❌ Excepción durante inicialización de JwtProvider: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    

    public String generateToken(JsonObject payload) {
        System.out.println("🔏 Generando token JWT con payload:");
        System.out.println(payload.encodePrettily());

        String token = jwtAuth.generateToken(payload, new JWTOptions().setExpiresInMinutes(60));
        System.out.println("🎟️ Token JWT generado exitosamente.");
        return token;
    }

    public JWTAuth getJwtAuth() {
        System.out.println("📦 Accediendo a instancia JWTAuth.");
        return jwtAuth;
    }
}
