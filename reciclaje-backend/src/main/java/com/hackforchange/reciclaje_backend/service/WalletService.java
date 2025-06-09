// WalletService.java
package com.hackforchange.reciclaje_backend.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import com.hackforchange.reciclaje_backend.repository.WalletRepository;
import com.hackforchange.reciclaje_backend.wallet.GoogleWalletService;

public class WalletService {
    private final WalletRepository repo;
    private final GoogleWalletService wallet;

    public WalletService(WalletRepository repo, GoogleWalletService wallet) {
        this.repo = repo;
        this.wallet = wallet;
    }

    public Future<String> generateLink(int userId) {
        Promise<String> p = Promise.promise();
        repo.findUserAndUid(userId).onSuccess(row -> {
            if (row == null) {
                p.fail("Usuario no encontrado");
                return;
            }
            String uid = row.getString("uid");
            String nombre = row.getString("nombre");
            if (uid == null) {
                p.fail("Necesitas tarjeta física");
                return;
            }
            try {
                String url = wallet.generateAddToWalletLink(uid, nombre);
                p.complete(url);
            } catch (Exception e) {
                p.fail(e);
            }
        }).onFailure(p::fail);
        return p.future();
    }
}
