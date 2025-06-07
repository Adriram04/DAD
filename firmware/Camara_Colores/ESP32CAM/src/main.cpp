/* Edge Impulse Arduino examples
 * Copyright (c) 2022 EdgeImpulse Inc.
 *
 * Permiso libre para usar, copiar, modificar, etc. (igual que en tu código original).
 *
 * Estas versiones están testeadas con el ESP32 Arduino Core 2.0.4
 */

/* Includes ---------------------------------------------------------------- */
#include <Bolsitas_AI_inferencing.h>
#include "edge-impulse-sdk/dsp/image/image.hpp"
#include <WiFi.h>
#include <WiFiClient.h>
#include <PubSubClient.h>
#include "esp_camera.h"

/* ----------------------- CONFIGURACIÓN DE CÁMARA ------------------------- */

// Selección del modelo de cámara (AI-THINKER para ESP32-CAM)
#define CAMERA_MODEL_AI_THINKER

#if defined(CAMERA_MODEL_AI_THINKER)
  #define PWDN_GPIO_NUM     32
  #define RESET_GPIO_NUM    -1
  #define XCLK_GPIO_NUM     0
  #define SIOD_GPIO_NUM     26
  #define SIOC_GPIO_NUM     27

  #define Y9_GPIO_NUM       35
  #define Y8_GPIO_NUM       34
  #define Y7_GPIO_NUM       39
  #define Y6_GPIO_NUM       36
  #define Y5_GPIO_NUM       21
  #define Y4_GPIO_NUM       19
  #define Y3_GPIO_NUM       18
  #define Y2_GPIO_NUM       5
  #define VSYNC_GPIO_NUM    25
  #define HREF_GPIO_NUM     23
  #define PCLK_GPIO_NUM     22
#else
  #error "Modelo de cámara no seleccionado"
#endif

/* Constant defines -------------------------------------------------------- */
// Tamaño original del frame buffer (RGB888) antes de redimensionar/filtro
#define EI_CAMERA_RAW_FRAME_BUFFER_COLS   320
#define EI_CAMERA_RAW_FRAME_BUFFER_ROWS   240
#define EI_CAMERA_FRAME_BYTE_SIZE         3

/* MQTT & WiFi configuration ------------------------------------------------ */
const char* WIFI_SSID     = "DIGIFIBRA-kGdz";
const char* WIFI_PASSWORD = "Rys5fKbPhTbc";
const char* MQTT_BROKER   = "172.201.114.19";  // Cambia por tu broker
const int   MQTT_PORT     = 1883;              // Puerto MQTT
const char* MQTT_TOPIC    = "proyecto/micro/colores";

// WiFi and MQTT clients
WiFiClient    wifiClient;
PubSubClient  client(wifiClient);

/* FreeRTOS queue para pasar payloads JSON de TaskInference a TaskMQTT */
static QueueHandle_t payloadQueue;

/* Variables para la cámara */
static bool is_initialised = false;
static uint8_t *snapshot_buf = nullptr; // buffer para RGB888 raw

/* Configuración fija de la cámara */
static camera_config_t camera_config = {
    .pin_pwdn       = PWDN_GPIO_NUM,
    .pin_reset      = RESET_GPIO_NUM,
    .pin_xclk       = XCLK_GPIO_NUM,
    .pin_sscb_sda   = SIOD_GPIO_NUM,
    .pin_sscb_scl   = SIOC_GPIO_NUM,

    .pin_d7         = Y9_GPIO_NUM,
    .pin_d6         = Y8_GPIO_NUM,
    .pin_d5         = Y7_GPIO_NUM,
    .pin_d4         = Y6_GPIO_NUM,
    .pin_d3         = Y5_GPIO_NUM,
    .pin_d2         = Y4_GPIO_NUM,
    .pin_d1         = Y3_GPIO_NUM,
    .pin_d0         = Y2_GPIO_NUM,
    .pin_vsync      = VSYNC_GPIO_NUM,
    .pin_href       = HREF_GPIO_NUM,
    .pin_pclk       = PCLK_GPIO_NUM,

    .xclk_freq_hz   = 20000000,
    .ledc_timer     = LEDC_TIMER_0,
    .ledc_channel   = LEDC_CHANNEL_0,

    .pixel_format   = PIXFORMAT_JPEG,
    .frame_size     = FRAMESIZE_QVGA, // 320x240 JPEG
    .jpeg_quality   = 12,
    .fb_count       = 1,
    .fb_location    = CAMERA_FB_IN_PSRAM,
    .grab_mode      = CAMERA_GRAB_WHEN_EMPTY,
};

/* Prototipos de funciones ------------------------------------------------- */
bool ei_camera_init(void);
bool ei_camera_capture(uint32_t img_width, uint32_t img_height, uint8_t *out_buf);
static int ei_camera_get_data(size_t offset, size_t length, float *out_ptr);
void connectToWiFi(void);
void connectToMQTT(void);

// Declaración de tareas FreeRTOS
void TaskInference(void* pvParameters);
void TaskMQTT(void* pvParameters);

/* =========================================================================== */
/* ================================ SETUP ==================================== */
/* =========================================================================== */
void setup() {
    Serial.begin(115200);
    while (!Serial) { /* Espera a que el puerto Serial esté listo */ }
    Serial.println("Inicializando ESP32-CAM con FreeRTOS...");

    // Inicializa la cámara
    if (!ei_camera_init()) {
        Serial.println("Fallo al inicializar la cámara. Deteniendo.");
        while (true) { vTaskDelay(pdMS_TO_TICKS(1000)); }
    }
    Serial.println("Cámara inicializada.");

    // Conecta a WiFi y MQTT antes de crear tareas
    connectToWiFi();
    connectToMQTT();

    // Crea la cola: capacidad para 5 mensajes de hasta 256 bytes cada uno
    payloadQueue = xQueueCreate(5, 256);
    if (payloadQueue == NULL) {
        Serial.println("Error al crear la cola FreeRTOS. Reinicia o revisa memoria.");
        while (true) { vTaskDelay(pdMS_TO_TICKS(1000)); }
    }

    // Crea TaskInference: stack 8K, prioridad 1, corriendo en APP_CPU (core 1)
    xTaskCreatePinnedToCore(
        TaskInference,
        "TaskInference",
        8192,       // bytes de stack
        NULL,
        1,          // prioridad baja
        NULL,
        APP_CPU_NUM // core 1
    );

    // Crea TaskMQTT: stack 6K, prioridad 2 (más alta que la inferencia), en core 1
    xTaskCreatePinnedToCore(
        TaskMQTT,
        "TaskMQTT",
        6144,
        NULL,
        2,          // prioridad un poco más alta
        NULL,
        APP_CPU_NUM
    );

    // No usamos loop() de Arduino; todo está en tareas
}

void loop() {
    // Se deja vacío. FreeRTOS gestiona todo.
    vTaskDelay(pdMS_TO_TICKS(1000));
}

/* =========================================================================== */
/* ====================== IMPLEMENTACIÓN DE FUNCIONES ======================= */
/* =========================================================================== */

/**
 * @brief   Inicializa la cámara con la configuración dada
 */
bool ei_camera_init(void) {
    if (is_initialised) return true;

    esp_err_t err = esp_camera_init(&camera_config);
    if (err != ESP_OK) {
        Serial.printf("Camera init failed con error 0x%x\n", err);
        return false;
    }

    sensor_t* s = esp_camera_sensor_get();
    if (s->id.PID == OV2640_PID) {
        s->set_framesize(s, FRAMESIZE_QVGA);
        s->set_pixformat(s, PIXFORMAT_JPEG);
        s->set_brightness(s, 1);
        s->set_contrast(s, 1);
        s->set_saturation(s, 2);
        s->set_sharpness(s, 2);
        s->set_quality(s, 12);
        s->set_gainceiling(s, (gainceiling_t)3);
        s->set_whitebal(s, 1);
        s->set_awb_gain(s, 1);
        s->set_exposure_ctrl(s, 1);
        s->set_aec2(s, 1);
        s->set_ae_level(s, 0);
        s->set_gain_ctrl(s, 1);
        s->set_agc_gain(s, 0);
        s->set_lenc(s, 1);
        s->set_hmirror(s, 0);
        s->set_vflip(s, 0);
    }

    is_initialised = true;
    return true;
}

/**
 * @brief   Captura, aplica filtro potencializado para rosa y azul, luego redimensiona si hace falta
 */
bool ei_camera_capture(uint32_t img_width, uint32_t img_height, uint8_t *out_buf) {
    if (!is_initialised) {
        ei_printf("ERR: Camera is not initialized\r\n");
        return false;
    }

    camera_fb_t *fb = esp_camera_fb_get();
    if (!fb) {
        ei_printf("Camera capture failed\n");
        return false;
    }

    // Convierte JPEG→RGB888 en snapshot_buf
    bool converted = fmt2rgb888(fb->buf, fb->len, PIXFORMAT_JPEG, snapshot_buf);
    esp_camera_fb_return(fb);
    if (!converted) {
        ei_printf("Conversion failed\n");
        return false;
    }

    // ─── FILTRO POTENCIADO PARA ROSA Y SOBRETODO AZUL ────────────────────────────
    size_t total_pixels = EI_CAMERA_RAW_FRAME_BUFFER_COLS * EI_CAMERA_RAW_FRAME_BUFFER_ROWS;
    for (size_t i = 0; i < total_pixels; i++) {
        uint8_t *r = &snapshot_buf[3*i + 0];
        uint8_t *g = &snapshot_buf[3*i + 1];
        uint8_t *b = &snapshot_buf[3*i + 2];

        // (1) Rosa: R alto, G y B bajos
        if (*r > 100 && *g < 120 && *b < 120) {
            int boostedR = *r + 50;
            *r = (uint8_t)((boostedR > 255) ? 255 : boostedR);
        }
        // (2) Azul: B domina por >40 sobre R y G
        else if (*b > 70 && (*b > *r + 40) && (*b > *g + 40)) {
            *b = 255;
            *r = (*r > 30) ? *r - 30 : 0;
            *g = (*g > 30) ? *g - 30 : 0;
        }
        // (3) Si no cumple condiciones, deja los valores tal cual
    }
    // ────────────────────────────────────────────────────────────────────────────

    // Redimensiona si las dimensiones deseadas son distintas
    bool do_resize = (img_width != EI_CAMERA_RAW_FRAME_BUFFER_COLS) ||
                     (img_height != EI_CAMERA_RAW_FRAME_BUFFER_ROWS);
    if (do_resize) {
        ei::image::processing::crop_and_interpolate_rgb888(
            out_buf,
            EI_CAMERA_RAW_FRAME_BUFFER_COLS,
            EI_CAMERA_RAW_FRAME_BUFFER_ROWS,
            out_buf,
            img_width,
            img_height
        );
    }

    return true;
}

/**
 * @brief   Obtiene datos convertidos en floats para el clasificador
 */
static int ei_camera_get_data(size_t offset, size_t length, float *out_ptr) {
    size_t pixel_ix = offset * 3;
    size_t pixels_left = length;
    size_t out_ptr_ix = 0;

    while (pixels_left != 0) {
        // BGR→RGB o empaquetado como entero; Edge Impulse espera floats
        out_ptr[out_ptr_ix] = (snapshot_buf[pixel_ix + 2] << 16)
                            + (snapshot_buf[pixel_ix + 1] << 8)
                            + (snapshot_buf[pixel_ix + 0]);

        out_ptr_ix++;
        pixel_ix += 3;
        pixels_left--;
    }
    return 0;
}

/**
 * @brief   Conecta a la red WiFi (bloqueante hasta conectar)
 */
void connectToWiFi(void) {
    Serial.print("Conectando a WiFi");
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nWiFi conectado. IP: " + WiFi.localIP().toString());
}

/**
 * @brief   Conecta al broker MQTT (bloqueante, reintenta hasta conectar)
 */
void connectToMQTT(void) {
    client.setServer(MQTT_BROKER, MQTT_PORT);
    while (!client.connected()) {
        Serial.print("Conectando a broker MQTT...");
        String clientId = "ESP32Client-" + String(random(0xffff), HEX);
        if (client.connect(clientId.c_str())) {
            Serial.println("conectado.");
        } else {
            Serial.print("falló, rc=");
            Serial.print(client.state());
            Serial.println(" reintentando en 5s...");
            delay(5000);
        }
    }
}

/* =========================================================================== */
/* ======================== TAREA DE INFERENCIA ============================= */
/* =========================================================================== */
void TaskInference(void* pvParameters) {
    // Asignamos un solo buffer para snapshot (RGB888) una vez
    snapshot_buf = (uint8_t*)malloc(
        EI_CAMERA_RAW_FRAME_BUFFER_COLS *
        EI_CAMERA_RAW_FRAME_BUFFER_ROWS *
        EI_CAMERA_FRAME_BYTE_SIZE
    );
    if (snapshot_buf == nullptr) {
        Serial.println("ERR: no se pudo asignar snapshot_buf. Abortando TaskInference.");
        vTaskDelete(NULL);
    }

    ei_printf("TaskInference arrancada.\r\n");

    // Bucle infinito de inferencia
    for (;;) {
        // 1) Captura + run_classifier
        ei::signal_t signal;
        signal.total_length = EI_CLASSIFIER_INPUT_WIDTH * EI_CLASSIFIER_INPUT_HEIGHT;
        signal.get_data = &ei_camera_get_data;

        if (!ei_camera_capture(
                EI_CLASSIFIER_INPUT_WIDTH,
                EI_CLASSIFIER_INPUT_HEIGHT,
                snapshot_buf)) {
            Serial.println("Fallo en captura. Reintentando en 200 ms...");
            vTaskDelay(pdMS_TO_TICKS(200));
            continue;
        }

        ei_impulse_result_t result = { 0 };
        EI_IMPULSE_ERROR err = run_classifier(&signal, &result, false);
        if (err != EI_IMPULSE_OK) {
            ei_printf("ERR: classifier falla (%d)\r\n", err);
            vTaskDelay(pdMS_TO_TICKS(200));
            continue;
        }

        // ── IMPRIMIR TIEMPOS DE DSP, CLASSIFICATION, ANOMALY ────────────────────
        ei_printf(
            "Predictions (DSP: %d ms., Classification: %d ms., Anomaly: %d ms.): \r\n",
            result.timing.dsp, result.timing.classification, result.timing.anomaly
        );

        // ── OBJETO DECTECTION VS CLASIFICACIÓN ─────────────────────────────────
#if EI_CLASSIFIER_OBJECT_DETECTION == 1
            // Si el modelo es de detección de objetos, imprimimos bounding boxes
    ei_printf("Object detection bounding boxes:\r\n");

    float max_value = 0.0f;
    const char* max_label = nullptr;

    for (uint32_t i = 0; i < result.bounding_boxes_count; i++) {
        ei_impulse_result_bounding_box_t bb = result.bounding_boxes[i];
        if (bb.value == 0) continue;

        ei_printf(
            "  %s (%f) [ x: %u, y: %u, width: %u, height: %u ]\r\n",
            bb.label,
            bb.value,
            bb.x, bb.y, bb.width, bb.height
        );

        // Guardar el que tenga mayor confianza
        if (bb.value > max_value) {
            max_value = bb.value;
            max_label = bb.label;
        }
    }

    if (max_label != nullptr) {
        // Crear el payload JSON solo con el color de mayor confianza
        char payload[64];
        snprintf(payload, sizeof(payload), "{\"color\":\"%s\"}", max_label);
        ei_printf("Payload: %s\r\n", payload);

        // Enviar a la cola
        if (xQueueSend(payloadQueue, payload, pdMS_TO_TICKS(100)) != pdPASS) {
            Serial.println("⚠️ Cola de payloads llena, se descarta payload.");
        } else {
            Serial.print("📤 Payload enviado a cola: ");
            Serial.println(payload);
        }
    }
#else
        // Si no es detección de objetos, imprimimos predicciones y armamos JSON
        ei_printf("Predictions:\r\n");

        // Construcción del payload JSON con todas las etiquetas
        char payload[256];
        int len = snprintf(payload, sizeof(payload), "{");
        for (uint16_t i = 0; i < EI_CLASSIFIER_LABEL_COUNT; i++) {
            ei_printf("  %s: %.5f\r\n",
                      ei_classifier_inferencing_categories[i],
                      result.classification[i].value);

            len += snprintf(payload + len, sizeof(payload) - len,
                            "\"%s\":%.5f%s",
                            ei_classifier_inferencing_categories[i],
                            result.classification[i].value,
                            (i < EI_CLASSIFIER_LABEL_COUNT - 1) ? ", " : "");
        }
        len += snprintf(payload + len, sizeof(payload) - len, "}");
        ei_printf("Payload: %s\r\n", payload);

        // Enviar el JSON a la cola (espera máximo 100 ms si está llena)
        if (xQueueSend(payloadQueue, payload, pdMS_TO_TICKS(100)) != pdPASS) {
            Serial.println("Warning: cola de payloads llena, se descarta payload.");
        } else {
            Serial.print("Payload enviado a cola: ");
            Serial.println(payload);
        }
#endif

        // ── ANOMALÍA ────────────────────────────────────────────────────────────
#if EI_CLASSIFIER_HAS_ANOMALY
        ei_printf("Anomaly prediction: %.3f\r\n", result.anomaly);
#endif

        // ── ANOMALÍA VISUAL ─────────────────────────────────────────────────────
#if EI_CLASSIFIER_HAS_VISUAL_ANOMALY
        ei_printf("Visual anomalies:\r\n");
        for (uint32_t i = 0; i < result.visual_ad_count; i++) {
            ei_impulse_result_bounding_box_t bb = result.visual_ad_grid_cells[i];
            if (bb.value == 0) continue;
            ei_printf(
                "  %s (%f) [ x: %u, y: %u, width: %u, height: %u ]\r\n",
                bb.label,
                bb.value,
                bb.x, bb.y, bb.width, bb.height
            );
        }
#endif
        // ────────────────────────────────────────────────────────────────────────

        // 4) Esperar antes de la próxima inferencia (200 ms aprox.)
        vTaskDelay(pdMS_TO_TICKS(200));
    }

    // Nunca debería llegar aquí, pero si sale, libera buffer y elimina la tarea
    free(snapshot_buf);
    vTaskDelete(NULL);
}

/* =========================================================================== */
/* =========================== TAREA MQTT =================================== */
/* =========================================================================== */
void TaskMQTT(void* pvParameters) {
    char incomingPayload[256];

    ei_printf("TaskMQTT arrancada.\r\n");

    for (;;) {
        // 1) Verificar que MQTT esté conectado; si no, reconectar
        if (!client.connected()) {
            Serial.println("MQTT desconectado. Reintentando conexión...");
            connectToMQTT();
        }
        client.loop(); // Mantiene viva la conexión y procesa callbacks

        // 2) Si hay payload en la cola, publicarlo
        if (xQueueReceive(payloadQueue, incomingPayload, pdMS_TO_TICKS(100)) == pdPASS) {
            boolean ok = client.publish(MQTT_TOPIC, incomingPayload);
            if (ok) {
                Serial.print("Publicado en MQTT: ");
                Serial.println(incomingPayload);
            } else {
                Serial.println("Error al publicar en MQTT.");
            }
        }

        // 3) Pequeña pausa (10 ms) para no bloquear el CPU
        vTaskDelay(pdMS_TO_TICKS(10));
    }

    // No debería llegar aquí nunca
    vTaskDelete(NULL);
}