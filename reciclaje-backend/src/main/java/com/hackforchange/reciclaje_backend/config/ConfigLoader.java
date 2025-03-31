package com.hackforchange.reciclaje_backend.config;

import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;

public class ConfigLoader {

	public static Future<JsonObject> load(Vertx vertx) {
	    Promise<JsonObject> promise = Promise.promise();

	    ConfigStoreOptions fileStore = new ConfigStoreOptions()
	        .setType("file")
	        .setFormat("json")
	        .setConfig(new JsonObject().put("path", "config.json"));

	    ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(fileStore));

	    retriever.getConfig(ar -> {
	        if (ar.succeeded()) {
	            promise.complete(ar.result());
	        } else {
	            promise.fail(ar.cause());
	        }
	    });

	    return promise.future();
	}

}
