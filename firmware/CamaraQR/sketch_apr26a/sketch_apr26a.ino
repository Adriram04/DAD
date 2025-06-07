/*********
  Rui Santos & Sara Santos - Random Nerd Tutorials
  Complete project details at https://RandomNerdTutorials.com/esp32-cam-qr-code-reader-scanner-arduino/
*********/

#include <Arduino.h>
#include <ESP32QRCodeReader.h>

// Librerías nuevas para MQTT y WiFi
#include <WiFi.h>
#include <PubSubClient.h>

// Wi-Fi
const char* ssid = "vivo V40 SE 5G";           // ← PON AQUÍ TU WIFI
const char* password = "uaz3fhfzdvufkav";   // ← PON AQUÍ TU CONTRASEÑA

// MQTT
const char* mqtt_server = "172.201.114.19";
const char* mqtt_topic = "proyecto/micro/qr";

WiFiClient espClient;
PubSubClient client(espClient);

ESP32QRCodeReader reader(CAMERA_MODEL_AI_THINKER);

// Función para conectar al WiFi
void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Conectando a ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi conectado");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}

// Función para conectar al broker MQTT
void reconnect() {
  // Bucle hasta que se conecte
  while (!client.connected()) {
    Serial.print("Conectando al broker MQTT...");
    if (client.connect("ESP32CAMClient")) {
      Serial.println("conectado!");
    } else {
      Serial.print("falló, rc=");
      Serial.print(client.state());
      Serial.println(" intentando de nuevo en 5 segundos");
      delay(5000);
    }
  }
}

void onQrCodeTask(void *pvParameters) {
  struct QRCodeData qrCodeData;

  while (true) {
    if (!client.connected()) {
      reconnect();
    }
    client.loop();

    if (reader.receiveQrCode(&qrCodeData, 100)) {
      Serial.println("Scanned new QRCode");
      if (qrCodeData.valid) {
        Serial.print("Valid payload: ");
        Serial.println((const char *)qrCodeData.payload);
        client.publish(mqtt_topic, (const char *)qrCodeData.payload);
      }
      else {
        Serial.print("Invalid payload: ");
        Serial.println((const char *)qrCodeData.payload);
        client.publish(mqtt_topic, "INVALID QR");
      }
    }
    vTaskDelay(100 / portTICK_PERIOD_MS);
  }
}

void setup() {
  Serial.begin(115200);
  Serial.println();

  setup_wifi();
  client.setServer(mqtt_server, 1883);

  reader.setup();
  Serial.println("Setup QRCode Reader");

  reader.beginOnCore(1);
  Serial.println("Begin on Core 1");

  xTaskCreate(onQrCodeTask, "onQrCode", 4 * 1024, NULL, 4, NULL);
}

void loop() {
  delay(100);
}
