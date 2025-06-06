package com.hackforchange.reciclaje_backend.wallet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class GoogleWalletService {

  private final String issuerId;
  private final String classId;
  private final RSAPrivateKey privateKey;
  private final String clientEmail;

  public GoogleWalletService(String issuerId, String classId, String saJsonPath) throws Exception {
    this.issuerId = issuerId;
    this.classId  = classId;

    ServiceAccountCredentials creds =
        ServiceAccountCredentials.fromStream(new FileInputStream(saJsonPath));
    this.privateKey  = (RSAPrivateKey) creds.getPrivateKey();
    this.clientEmail = creds.getClientEmail();
  }

  /** Devuelve el link “Add to Google Wallet” (modo Demo) */
  public String generateAddToWalletLink(String uid, String nombre) {
    String objectId = issuerId + "." + uid;

    /* 1) Cuerpo mínimo del objeto Loyalty */
    Map<String,Object> lo = Map.of(
        "id",          objectId,
        "classId",     classId,
        "accountId",   uid,
        "accountName", nombre,
        "barcode", Map.of(
            "type",          "QR_CODE",
            "value",         uid,
            "alternateText", uid)
    );

    /* 2) JWT firmado */
    Instant now = Instant.now();

    String jwt = JWT.create()
        .withIssuer(clientEmail)
        .withAudience("google")
        .withIssuedAt(java.util.Date.from(now))
        .withExpiresAt(java.util.Date.from(now.plusSeconds(3600)))
        /* ❷  usa una List para que resulte un array JSON */
        .withClaim("loyaltyObjects", List.of(lo))
        .sign(Algorithm.RSA256(null, privateKey));


    return "https://pay.google.com/gp/v/save/" + jwt;
  }
}
