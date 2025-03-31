package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class ProductosController {

    private final MySQLPool client;

    public ProductosController(MySQLPool client) {
        this.client = client;
    }

    public void getRouter(Router router) {
        // Definimos las rutas CRUD
        router.get("/productos").handler(this::handleListProductos);
        router.get("/productos/:id").handler(this::handleGetProducto);
        router.post("/productos").handler(this::handleCreateProducto);
        router.put("/productos/:id").handler(this::handleUpdateProducto);
        router.delete("/productos/:id").handler(this::handleDeleteProducto);
    }

    /**
     * GET /productos
     * Devuelve la lista de todos los productos.
     */
    private void handleListProductos(RoutingContext ctx) {
        String sql = "SELECT p.id, p.nombre, p.puntos_necesarios, p.id_proveedor " +
                     "FROM producto p";

        client.query(sql).execute(ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                JsonArray productos = new JsonArray();
                for (Row row : rows) {
                    JsonObject prod = new JsonObject()
                        .put("id", row.getInteger("id"))
                        .put("nombre", row.getString("nombre"))
                        .put("puntos_necesarios", row.getInteger("puntos_necesarios"))
                        .put("id_proveedor", row.getInteger("id_proveedor"));
                    productos.add(prod);
                }

                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("productos", productos).encode());
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al obtener productos");
            }
        });
    }

    /**
     * GET /productos/:id
     * Devuelve un producto dado su id.
     */
    private void handleGetProducto(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ ID inválido");
            return;
        }

        String sql = "SELECT p.id, p.nombre, p.puntos_necesarios, p.id_proveedor " +
                     "FROM producto p WHERE p.id = ?";

        client.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                if (!result.iterator().hasNext()) {
                    ctx.response().setStatusCode(404).end("❌ Producto no encontrado");
                    return;
                }
                Row row = result.iterator().next();
                JsonObject prod = new JsonObject()
                        .put("id", row.getInteger("id"))
                        .put("nombre", row.getString("nombre"))
                        .put("puntos_necesarios", row.getInteger("puntos_necesarios"))
                        .put("id_proveedor", row.getInteger("id_proveedor"));

                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(prod.encode());
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al obtener producto");
            }
        });
    }

    /**
     * POST /productos
     * Crea un nuevo producto. El body debe contener JSON con:
     * {
     *   "nombre": "...",
     *   "puntos_necesarios": 123,
     *   "id_proveedor": 3
     * }
     */
    private void handleCreateProducto(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("❌ Cuerpo JSON requerido");
            return;
        }

        String nombre = body.getString("nombre");
        Integer puntos = body.getInteger("puntos_necesarios");
        Integer idProveedor = body.getInteger("id_proveedor");

        // Validaciones simples
        if (nombre == null || puntos == null || idProveedor == null) {
            ctx.response().setStatusCode(400).end("❌ Faltan campos necesarios");
            return;
        }

        String sql = "INSERT INTO producto (nombre, puntos_necesarios, id_proveedor) VALUES (?, ?, ?)";

        client.preparedQuery(sql).execute(Tuple.of(nombre, puntos, idProveedor), ar -> {
            if (ar.succeeded()) {
                // Podríamos también recuperar el ID autogenerado si lo necesitamos
                ctx.response().setStatusCode(201).end("✅ Producto creado");
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al crear producto");
            }
        });
    }

    /**
     * PUT /productos/:id
     * Actualiza un producto existente.
     * El body debe contener JSON con:
     * {
     *   "nombre": "...",
     *   "puntos_necesarios": 123,
     *   "id_proveedor": 3
     * }
     */
    private void handleUpdateProducto(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ ID inválido");
            return;
        }

        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("❌ Cuerpo JSON requerido");
            return;
        }

        String nombre = body.getString("nombre");
        Integer puntos = body.getInteger("puntos_necesarios");
        Integer idProveedor = body.getInteger("id_proveedor");

        // Validaciones simples
        if (nombre == null || puntos == null || idProveedor == null) {
            ctx.response().setStatusCode(400).end("❌ Faltan campos necesarios");
            return;
        }

        String sql = "UPDATE producto SET nombre = ?, puntos_necesarios = ?, id_proveedor = ? WHERE id = ?";

        client.preparedQuery(sql).execute(Tuple.of(nombre, puntos, idProveedor, id), ar -> {
            if (ar.succeeded()) {
                if (ar.result().rowCount() == 0) {
                    ctx.response().setStatusCode(404).end("❌ Producto no encontrado");
                } else {
                    ctx.response().end("✅ Producto actualizado");
                }
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al actualizar producto");
            }
        });
    }

    /**
     * DELETE /productos/:id
     * Elimina un producto por su id
     */
    private void handleDeleteProducto(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("❌ ID inválido");
            return;
        }

        String sql = "DELETE FROM producto WHERE id = ?";

        client.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                if (ar.result().rowCount() == 0) {
                    ctx.response().setStatusCode(404).end("❌ Producto no encontrado");
                } else {
                    ctx.response().end("✅ Producto eliminado");
                }
            } else {
                ctx.response().setStatusCode(500).end("❌ Error al eliminar producto");
            }
        });
    }
}
