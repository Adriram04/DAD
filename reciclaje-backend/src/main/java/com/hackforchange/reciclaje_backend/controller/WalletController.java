package com.hackforchange.reciclaje_backend.controller;

import com.hackforchange.reciclaje_backend.wallet.GoogleWalletService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class WalletController {
  private final MySQLPool client;
  private final GoogleWalletService wallet;
  public WalletController(MySQLPool c, GoogleWalletService w){ client=c; wallet=w; }

  public void getRouter(Router router){
    router.get("/wallet/link").handler(this::handleLink);
  }

  private void handleLink(RoutingContext ctx){
    int userId = Integer.parseInt(ctx.queryParam("userId").get(0));

    String sql = "SELECT u.nombre, t.uid FROM usuario u " +
                 "LEFT JOIN tarjeta t ON t.id_consumidor=u.id WHERE u.id=? LIMIT 1";

    client.preparedQuery(sql).execute(Tuple.of(userId), ar -> {
      if(ar.failed()||ar.result().size()==0){ ctx.fail(404); return; }
      Row r = ar.result().iterator().next();
      String uid = r.getString("uid");
      if(uid==null){
        ctx.response().setStatusCode(409)
           .end(new JsonObject().put("error","Necesitas tarjeta física").encode());
        return;
      }
      try{
        String url = wallet.generateAddToWalletLink(uid, r.getString("nombre"));
        ctx.json(new JsonObject().put("url",url));
      }catch(Exception e){ ctx.fail(e); }
    });
  }
}
