// TarjetaService.java
package com.hackforchange.reciclaje_backend.service;

import io.vertx.core.Future;
import com.hackforchange.reciclaje_backend.repository.TarjetaRepository;

public class TarjetaService {
    private final TarjetaRepository repo;

    public TarjetaService(TarjetaRepository repo) {
        this.repo = repo;
    }

    public Future<Void> createTarjeta(String uid, Integer consumidorId) {
        if (uid == null || consumidorId == null) {
            return Future.failedFuture("uid e id_consumidor son obligatorios");
        }
        return repo.insertTarjeta(uid, consumidorId);
    }
}
