package com.hackforchange.reciclaje_backend.controller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.Row;

public class MqttService extends AbstractVerticle {

    private final MySQLPool client;
    private MqttClient mqtt;
    private final String brokerHost;
    private final int    brokerPort;

    public MqttService(MySQLPool client, String host, int port) {
        this.client     = client;
        this.brokerHost = host;
        this.brokerPort = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        mqtt = MqttClient.create(vertx, new MqttClientOptions());
        mqtt.connect(brokerPort, brokerHost, s -> {
            if (s.succeeded()) {
                System.out.println("✅ MQTT conectado a " + brokerHost + ":" + brokerPort);
                subscribeTopics();
                startPromise.complete();
            } else {
                startPromise.fail(s.cause());
            }
        });
    }

    /* ───────────────────── SUBS ───────────────────── */
    private void subscribeTopics() {
        String[] topics = {
            "proyecto/micro/puntos",
            "proyecto/micro/sensores"
        };
        for (String t : topics) mqtt.subscribe(t, 1);
        mqtt.publishHandler(this::handleMessage);
    }

    /* ───────────────────── HANDLER ─────────────────── */
    private void handleMessage(MqttPublishMessage msg) {
        String     topic   = msg.topicName();
        JsonObject payload = msg.payload().toJsonObject();

        if ("proyecto/micro/puntos".equals(topic)) {
            procesarReciclaje(payload);
        } else if ("proyecto/micro/sensores".equals(topic)) {
            if (payload.containsKey("temperatura")) {
                evaluarTemperatura(payload.getFloat("temperatura"));
            }
        }
    }

    /* ==========   Lógica de reciclaje ========== */
    private void procesarReciclaje(JsonObject p) {
        int    idConsumidor = p.getInteger("user"); // ID de usuario
        int    idContenedor = p.getInteger("id");   // ID de contenedor
        float  kg           = p.getFloat("peso");
        String color        = p.getString("color");
        String qr           = p.getString("qr");

        /* Color -> tipo de basura (switch clásico) */
        String tipoBasura;
        switch (color.toLowerCase()) {
            case "azul":
                tipoBasura = "PLASTICO";
                break;
            case "rosa":
                tipoBasura = "PAPEL";
                break;
            case "gris":
                tipoBasura = "VIDRIO";
                break;
            default:
                tipoBasura = "OTRO";
        }

        /* 1) Datos actuales del contenedor */
        client.preparedQuery(
            "SELECT capacidad_maxima, carga_actual FROM contenedor WHERE id=? LIMIT 1")
              .execute(Tuple.of(idContenedor), ar -> {

            if (ar.failed() || ar.result().size() == 0) return;

            Row   row         = ar.result().iterator().next();
            float nuevaCarga  = row.getFloat("carga_actual") + kg;
            float capacidad   = row.getFloat("capacidad_maxima");
            float pct         = nuevaCarga / capacidad;

            boolean lleno    = pct >= 0.75f;  // 75-90 %
            boolean bloqueo  = pct >= 0.90f;  // ≥ 90 %

            int puntos = calcularPuntos(tipoBasura, kg);

            /* 2) Transacción: registro + contenedor + usuario */
            client.withTransaction(tx ->
                // a) registro_reciclaje
                tx.preparedQuery(
                    "INSERT INTO registro_reciclaje " +
                    "(id_consumidor,id_contenedor,qr,tipo_basura,peso_kg,puntos_obtenidos) " +
                    "VALUES (?,?,?,?,?,?)")
                  .execute(Tuple.of(idConsumidor, idContenedor, qr,
                                    tipoBasura, kg, puntos))

                // b) contenedor
                .compose(v -> tx.preparedQuery(
                    "UPDATE contenedor SET carga_actual=?, lleno=?, bloqueo=? WHERE id=?")
                  .execute(Tuple.of(nuevaCarga, lleno, bloqueo, idContenedor)))

                // c) usuario
                .compose(v -> tx.preparedQuery(
                    "UPDATE usuario SET puntos = puntos + ? WHERE id=?")
                  .execute(Tuple.of(puntos, idConsumidor)))
            )
            /* 3) Notificación en tiempo real (solo si la tx fue bien) */
            .onSuccess(v -> {
                /* al usuario */
                JsonObject msgUsr = new JsonObject()
                        .put("puntosGanados", puntos)
                        .put("kg", kg);
                publish("ui/usuarios/" + idConsumidor + "/puntos", msgUsr);

                /* al contenedor */
                JsonObject msgCont = new JsonObject()
                        .put("carga_actual", nuevaCarga)
                        .put("lleno", lleno)
                        .put("bloqueo", bloqueo);
                publish("ui/contenedores/" + idContenedor, msgCont);
            })
            .onFailure(Throwable::printStackTrace);
        });
    }

    /* ==========  Temperatura -> bloqueo global ========== */
    private void evaluarTemperatura(float temperatura) {
        boolean bloquear = temperatura >= 40.0f;
        client.preparedQuery("UPDATE contenedor SET bloqueo=?")
              .execute(Tuple.of(bloquear), ar -> {});
    }

    /* ────────── Helper puntos (switch clásico) ────────── */
    private int calcularPuntos(String tipo, float kg) {
        int factor;
        switch (tipo) {
            case "PLASTICO":
                factor = 5;
                break;
            case "PAPEL":
                factor = 3;
                break;
            case "VIDRIO":
                factor = 2;
                break;
            default:
                factor = 1;
        }
        return Math.round(factor * kg);
    }

    /* ────────── Publicar genérico ────────── */
    public void publish(String topic, JsonObject message) {
        mqtt.publish(topic,
                     message.toBuffer(),
                     MqttQoS.AT_LEAST_ONCE,
                     false,
                     false);
    }
}
