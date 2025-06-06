package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.handler.BodyHandler;

public class ChatController {

  private final Vertx vertx;
  private final WebClient client;
  private final String openaiKey;

  public ChatController(Vertx vertx) {
    this.vertx = vertx;
    this.client = WebClient.create(vertx);
    // Lee la API key de OpenAI desde variable de entorno
    this.openaiKey = System.getenv("OPENAI_API_KEY");
    if (this.openaiKey == null || this.openaiKey.isEmpty()) {
      throw new RuntimeException("Debe definir la variable de entorno OPENAI_API_KEY");
    }
  }

  /**
   * Registra la ruta POST /api/eco-chat en el router principal.
   */
  public void getRouter(Router router) {
    // Necesitamos BodyHandler para leer JSON del cuerpo
    router.post("/api/eco-chat")
      .handler(BodyHandler.create())
      .handler(this::handleEcoChat);
  }

  /**
   * Maneja la petición POST /api/eco-chat.
   * Espera JSON: { message: string, history: [ { sender, text }, … ] }
   * Responde JSON: { reply: string }
   */
  private void handleEcoChat(RoutingContext ctx) {
    JsonObject body = ctx.getBodyAsJson();
    if (body == null) {
      ctx.response().setStatusCode(400).end("❌ JSON requerido");
      return;
    }

    String message = body.getString("message", "").trim();
    JsonArray history = body.getJsonArray("history", new JsonArray());

    if (message.isEmpty()) {
      ctx.response().setStatusCode(400).end("❌ El campo 'message' no puede estar vacío");
      return;
    }

    // Construir el array de mensajes para OpenAI
    JsonArray openaiMessages = new JsonArray();

    // Convertimos cada elemento de history en {"role": "user"|"assistant", "content": "..."}
    for (int i = 0; i < history.size(); i++) {
      JsonObject entry = history.getJsonObject(i);
      String sender = entry.getString("sender", "");
      String text = entry.getString("text", "");
      if (sender.equalsIgnoreCase("Usuario")) {
        openaiMessages.add(new JsonObject()
          .put("role", "user")
          .put("content", text));
      } else if (sender.equalsIgnoreCase("Bot")) {
        openaiMessages.add(new JsonObject()
          .put("role", "assistant")
          .put("content", text));
      }
    }

    // Añadimos el mensaje nuevo del usuario al final
    openaiMessages.add(new JsonObject()
      .put("role", "user")
      .put("content", message));

    // Armar JSON para la petición a OpenAI
    JsonObject openaiRequest = new JsonObject()
      .put("model", "gpt-3.5-turbo")
      .put("messages", openaiMessages)
      .put("max_tokens", 300)
      .put("temperature", 0.8);

    // Llamamos a la API de OpenAI
    client.post(443, "api.openai.com", "/v1/chat/completions")
      .ssl(true)
      .putHeader("Authorization", "Bearer " + openaiKey)
      .putHeader("Content-Type", "application/json")
      .sendJsonObject(openaiRequest, ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response.statusCode() == 200) {
            JsonObject data = response.bodyAsJsonObject();
            // Extraemos el contenido de la primera respuesta
            try {
              JsonArray choices = data.getJsonArray("choices");
              if (choices != null && choices.size() > 0) {
                JsonObject first = choices.getJsonObject(0);
                JsonObject messageObj = first.getJsonObject("message");
                String reply = messageObj.getString("content", "").trim();

                // Devolvemos al cliente
                ctx.response()
                  .putHeader("Content-Type", "application/json")
                  .end(new JsonObject().put("reply", reply).encode());
                return;
              }
            } catch (Exception e) {
              // Caída al parsear
              e.printStackTrace();
            }
            // Si no viene el campo esperado
            ctx.response().setStatusCode(502).end("❌ Error al parsear respuesta de OpenAI");
          } else {
            // La API de OpenAI devolvió un error
            ctx.response().setStatusCode(502)
              .end("❌ OpenAI respondió con código " + response.statusCode());
          }
        } else {
          // Falla de red o similar
          ar.cause().printStackTrace();
          ctx.response().setStatusCode(502).end("❌ Fallo llamando a OpenAI: " + ar.cause().getMessage());
        }
      });
  }
}
