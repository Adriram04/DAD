package com.hackforchange.reciclaje_backend.service;

import io.vertx.core.json.JsonObject;
import io.vertx.core.Future;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.sqlclient.Row;
import com.hackforchange.reciclaje_backend.repository.MqttRepository;

public class MqttService {
    private final MqttClient mqtt;
    private final MqttRepository repo;

    public MqttService(MqttClient mqtt, MqttRepository repo) {
        this.mqtt = mqtt;
        this.repo = repo;
    }

    public void subscribeTopics() {
        String[] topics = { "proyecto/micro/puntos", "proyecto/micro/sensores" };
        for (String t : topics) {
            mqtt.subscribe(t, 1);
        }
        mqtt.publishHandler(this::handleMessage);
    }

    private void handleMessage(MqttPublishMessage msg) {
        String topic = msg.topicName();
        JsonObject payload = msg.payload().toJsonObject();
        if ("proyecto/micro/puntos".equals(topic)) {
            procesarReciclaje(payload);
        } else if ("proyecto/micro/sensores".equals(topic) && payload.containsKey("temperatura")) {
            evaluarTemperatura(payload.getFloat("temperatura"));
        }
    }

    private void procesarReciclaje(JsonObject p) {
        int idUsuario    = p.getInteger("user");
        int idContenedor = p.getInteger("id");
        float kg         = p.getFloat("peso");
        String color     = p.getString("color");
        String qr        = p.getString("qr");

        // Color -> tipo de basura
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

        repo.fetchContenedor(idContenedor).onSuccess(row -> {
            if (row == null) {
                return;
            }
            float cargaActual = row.getFloat("carga_actual");
            float capacidad   = row.getFloat("capacidad_maxima");
            float nuevaCarga  = cargaActual + kg;
            float pct         = nuevaCarga / capacidad;
            boolean lleno     = pct >= 0.75f;
            boolean bloqueo   = pct >= 0.90f;
            int puntos        = calcularPuntos(tipoBasura, kg);

            repo.processReciclaje(idUsuario, idContenedor, qr, tipoBasura, kg, puntos,
                                  nuevaCarga, lleno, bloqueo)
                .onSuccess(v -> {
                    publish("ui/usuarios/" + idUsuario + "/puntos",
                            new JsonObject().put("puntosGanados", puntos).put("kg", kg));
                    publish("ui/contenedores/" + idContenedor,
                            new JsonObject()
                                .put("carga_actual", nuevaCarga)
                                .put("lleno", lleno)
                                .put("bloqueo", bloqueo));
                })
                .onFailure(Throwable::printStackTrace);
        });
    }

    private void evaluarTemperatura(float temperatura) {
        boolean bloquear = temperatura >= 40.0f;
        repo.updateTemperaturaGlobal(bloquear).onFailure(Throwable::printStackTrace);
    }

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

    private void publish(String topic, JsonObject msg) {
        mqtt.publish(topic, msg.toBuffer(), MqttQoS.AT_LEAST_ONCE, false, false);
    }
}
