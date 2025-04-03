package com.hackforchange.reciclaje_backend.database;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class MySQLClientProvider {

    public static MySQLPool createMySQLPool(Vertx vertx, JsonObject config) {
        JsonObject dbConfig = config.getJsonObject("db");

        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                .setPort(dbConfig.getInteger("port"))
                .setHost(dbConfig.getString("host"))
                .setDatabase(dbConfig.getString("database"))
                .setUser(dbConfig.getString("user"))
                .setPassword(dbConfig.getString("password"))
                .setSsl(true) 
                .setTrustAll(true)
                .setSslMode("DISABLED");

        PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

        return MySQLPool.pool(vertx, connectOptions, poolOptions);
    }
}
