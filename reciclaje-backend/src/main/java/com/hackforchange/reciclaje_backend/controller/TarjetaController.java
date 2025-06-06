package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Tuple;

public class TarjetaController {
  private final MySQLPool client;
  public TarjetaController(MySQLPool c){ this.client=c; }
  public void getRouter(Router router){
    router.post("/tarjetas").handler(this::handleCreate);
  }
  private void handleCreate(RoutingContext ctx){
    JsonObject b = ctx.body().asJsonObject();
    String uid  = b.getString("uid");
    Integer cid = b.getInteger("id_consumidor");
    if(uid==null||cid==null){ ctx.fail(400); return; }
    client.preparedQuery(
      "INSERT INTO tarjeta(uid,id_consumidor) VALUES(?,?)")
      .execute(Tuple.of(uid,cid), ar -> {
        if(ar.succeeded()) ctx.response().setStatusCode(201).end();
        else ctx.fail(ar.cause());
      });
  }
}
