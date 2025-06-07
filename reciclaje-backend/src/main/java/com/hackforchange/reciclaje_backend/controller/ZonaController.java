package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

/**
 * CRUD sencillo de zonas.  La columna `canal_mqtt` se ha eliminado de la tabla,
 * por lo que aquí ya no se referencia en ningún punto.
 */
public class ZonaController {

    private final MySQLPool client;
    public  ZonaController(MySQLPool client){ this.client = client; }

    /* ------------------------------------------------------------------ */
    public Router getRouter(Router router){
        router.get   ("/zonas")                       .handler(this::handleList);
        router.get   ("/zonas/with-contenedores")     .handler(this::handleZonasWithContenedorCount);
        router.post  ("/zonas")                       .handler(this::handleCreate);
        router.put   ("/zonas/:id")                   .handler(this::handleUpdate);
        router.delete("/zonas/:id")                   .handler(this::handleDelete);
        return router;
    }

    /* ---------------------- GET /zonas ------------------------------- */
    private void handleList(RoutingContext ctx){
        client.query("SELECT id,nombre FROM zona").execute(ar -> {
            if(ar.succeeded()){
                JsonArray arr = new JsonArray();
                for(Row r : ar.result())
                    arr.add(new JsonObject()
                               .put("id",     r.getInteger("id"))
                               .put("nombre", r.getString("nombre")));
                ctx.json(new JsonObject().put("zonas", arr));
            }else ctx.fail(ar.cause());
        });
    }

    /* ------------- GET /zonas/with-contenedores ---------------------- */
    private void handleZonasWithContenedorCount(RoutingContext ctx){
        /* 1) Traemos todas las zonas */
        client.query("SELECT id,nombre FROM zona").execute(zRes -> {
            if(!zRes.succeeded()){ ctx.fail(zRes.cause()); return; }

            JsonArray zonas = new JsonArray();
            zRes.result().forEach(r -> zonas.add(
                new JsonObject().put("id",r.getInteger("id"))
                                .put("nombre", r.getString("nombre")) ));

            /* 2) Traemos contenedores para añadirlos a su zona */
            client.query("SELECT id,nombre,id_zona,lleno,bloqueo FROM contenedor")
                  .execute(cRes -> {
                if(!cRes.succeeded()){ ctx.fail(cRes.cause()); return; }

                for(Row c : cRes.result()){
                    int zonaId = c.getInteger("id_zona");
                    JsonObject cont = new JsonObject()
                        .put("id",     c.getInteger("id"))
                        .put("nombre", c.getString("nombre"))
                        .put("lleno",  c.getBoolean("lleno"))
                        .put("bloqueo",c.getBoolean("bloqueo"));

                    // localizar zona en el array
                    for(int i=0;i<zonas.size();i++){
                        JsonObject z = zonas.getJsonObject(i);
                        if(z.getInteger("id")==zonaId){
                            z.getJsonArray("contenedores", new JsonArray())
                             .add(cont);
                            break;
                        }
                    }
                }

                ctx.json(new JsonObject().put("zonas", zonas));
            });
        });
    }

    /* --------------------- POST /zonas ------------------------------- */
    private void handleCreate(RoutingContext ctx){
        JsonObject b = ctx.body().asJsonObject();
        String nombre = b.getString("nombre");
        if(nombre==null || nombre.isBlank()){ ctx.fail(400); return; }

        client.preparedQuery("INSERT INTO zona(nombre) VALUES(?)")
              .execute(Tuple.of(nombre), ar -> {
                  if(ar.succeeded()) ctx.response().setStatusCode(201).end();
                  else ctx.fail(ar.cause());
              });
    }

    /* ---------------------- PUT /zonas/:id --------------------------- */
    private void handleUpdate(RoutingContext ctx){
        int id;
        try{ id = Integer.parseInt(ctx.pathParam("id")); }
        catch(NumberFormatException e){ ctx.fail(400); return; }

        JsonObject b = ctx.body().asJsonObject();
        String nombre = b.getString("nombre");
        if(nombre==null || nombre.isBlank()){ ctx.fail(400); return; }

        client.preparedQuery("UPDATE zona SET nombre=? WHERE id=?")
              .execute(Tuple.of(nombre,id), ar -> {
                  if(ar.succeeded())
                      ctx.response().end(
                          ar.result().rowCount()==0 ? "Zona no encontrada"
                                                    : "Zona actualizada");
                  else ctx.fail(ar.cause());
              });
    }

    /* ------------------- DELETE /zonas/:id --------------------------- */
    private void handleDelete(RoutingContext ctx){
        int id;
        try{ id = Integer.parseInt(ctx.pathParam("id")); }
        catch(NumberFormatException e){ ctx.fail(400); return; }

        client.preparedQuery("DELETE FROM zona WHERE id=?")
              .execute(Tuple.of(id), ar -> {
                  if(ar.succeeded())
                      ctx.response().end(
                          ar.result().rowCount()==0 ? "Zona no encontrada"
                                                    : "Zona eliminada");
                  else ctx.fail(ar.cause());
              });
    }
}
