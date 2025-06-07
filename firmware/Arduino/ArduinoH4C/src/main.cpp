// CODIGO PROYECTO ECOBINS (versión completa · 06-Jun-2025)
// ------------------------------------------------------------------
#include <MFRC522.h>
#include <LiquidCrystal_I2C.h>
#include <WiFiS3.h>
#include <PubSubClient.h>
#include <ArduinoHttpClient.h>
#include <ArduinoJson.h>

// ---------------- Pin definitions ----------------------------------
#define RST_PIN     9
#define SS_PIN      10
#define BUTTON_PIN  2
#define MOTOR_PIN_1 7
#define MOTOR_PIN_2 8

// ---------------- Hardware objects ---------------------------------
MFRC522 mfrc522(SS_PIN, RST_PIN);
LiquidCrystal_I2C lcd(0x27, 20, 4);

// ---------------- Wi-Fi & MQTT -------------------------------------
const char* ssid       = "vivo V40 SE 5G";
const char* password   = "uaz3fhfzdvufkav";
const char* mqttServer = "172.201.114.19";
const int   mqttPort   = 1883;
const char* mqttUser   = "mqtt_user";
const char* mqttPass   = "mqtt_pass";

// ---------------- HTTPS endpoints -----------------------------------
const char* apiHost = "api.ecobins.tech";
const int   apiPort = 443;

const char* apiPath = "/health/";

const char* apiPath2 = "/capacity/";

// ---------------- Client objects -----------------------------------
WiFiSSLClient   httpsTransport;
HttpClient      httpClient(httpsTransport, apiHost, apiPort);
WiFiClient      netClient;
PubSubClient    mqttClient(netClient);

// ---------------- Estado y flujo -----------------------------------
bool    authorized      = false;
bool    motorActive     = false;
unsigned long lastDisplayMillis = 0;
const unsigned long displayTimeout = 2000;
String  username        = "";
String  activeUID       = "";

int     colorValue      = 3;
volatile byte  contador           = 0;
int            storedColor        = 3;
volatile bool  flagLeerPeso       = false;
volatile bool  flagLeerColor      = false;
volatile bool  flagReciclarBolsa  = false;

String lastQR  = "";
int    peso    = 0;
int    storedPeso = 0;

byte recycleIcon[8] = {
  0b11111, 0b10101, 0b10101, 0b10101,
  0b10001, 0b11111, 0b00100, 0b00100
};

// ---------------- Prototipos ----------------------------------------
void handleRFID();
void connectWiFi();
void connectMQTT();
void mqttCallback(char* topic, byte* payload, unsigned int length);
void buttonISR();
void sendColorPulse();
bool checkCapacidad(); 

// ===================================================================
//                               SETUP
// ===================================================================
void setup() {
  Serial.begin(115200);
  SPI.begin();
  mfrc522.PCD_Init();

  lcd.init();
  lcd.createChar(0, recycleIcon);
  lcd.backlight();
  lcd.print("Esperando tarjeta");

  pinMode(BUTTON_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(BUTTON_PIN), buttonISR, FALLING);

  pinMode(MOTOR_PIN_1, OUTPUT);
  pinMode(MOTOR_PIN_2, OUTPUT);
  digitalWrite(MOTOR_PIN_1, HIGH);
  digitalWrite(MOTOR_PIN_2, HIGH);

  connectWiFi();
  mqttClient.setServer(mqttServer, mqttPort);
  mqttClient.setCallback(mqttCallback);
}

// ===================================================================
//                                LOOP
// ===================================================================
void loop() {
  if (!mqttClient.connected()) connectMQTT();
  mqttClient.loop();
  handleRFID();

  unsigned long now = millis();
  if (!authorized) {
    if (now - lastDisplayMillis > displayTimeout) {
      lcd.clear();
      lcd.print("Esperando tarjeta");
      lastDisplayMillis = now;
    }
    delay(10);
    return;
  }

  if (flagLeerPeso) {
    flagLeerPeso = false;
    storedPeso   = peso;
    lcd.clear(); lcd.print("Peso medido");
    delay(1500);
    lcd.clear(); lcd.print("Pulsa boton para");
    lcd.setCursor(0, 1); lcd.print("identificar color");
  }

  if (flagLeerColor) {
    flagLeerColor = false;
    storedColor   = colorValue;
    String colorStr = (storedColor == 3) ? "Rosa" :
                      (storedColor == 1) ? "Azul" :
                      (storedColor == 2) ? "Gris" : "Desconocido";
    lcd.clear(); lcd.print("Color detectado:");
    lcd.setCursor(0, 1); lcd.print(colorStr);
    delay(1000);
    lcd.clear(); lcd.print("Pulsa boton para");
    lcd.setCursor(0, 1); lcd.print("tirar bolsa");
  }

  if (flagReciclarBolsa) {
    flagReciclarBolsa = false;
    sendColorPulse();

    String colorStr = (storedColor == 3) ? "Rosa" :
                      (storedColor == 1) ? "Azul" :
                      (storedColor == 2) ? "Gris" : "Desconocido";

    lcd.clear(); lcd.print("Bolsa "); lcd.print(colorStr); lcd.print(" reciclada");
    delay(1500);

    StaticJsonDocument<256> outDoc;
    outDoc["user"]  = activeUID;
    outDoc["qr"]    = lastQR;
    outDoc["peso"]  = storedPeso;
    outDoc["color"] = colorStr;
    outDoc["id"] = 1;
    String outStr; serializeJson(outDoc, outStr);
    mqttClient.publish("proyecto/micro/puntos", outStr.c_str());
    delay(500);

    lcd.clear(); lcd.print("Escanea QR");
    lastDisplayMillis = millis();
    storedColor = 3; storedPeso = 0; lastQR = ""; contador = 0;
  }

  delay(10);
}

// ===================================================================
//                      FUNCIONES DE SOPORTE
// ===================================================================
void sendColorPulse() {
  Serial.print("Enviando colorValue: "); Serial.println(colorValue);
  int bitMSB = (colorValue >> 1) & 0x01;
  int bitLSB =  colorValue       & 0x01;
  digitalWrite(MOTOR_PIN_1, bitMSB ? LOW : HIGH);
  digitalWrite(MOTOR_PIN_2, bitLSB ? LOW : HIGH);
  delay(1000);
  digitalWrite(MOTOR_PIN_1, HIGH);
  digitalWrite(MOTOR_PIN_2, HIGH);
}

void connectWiFi() {
  lcd.clear(); lcd.print("Conectando WiFi");
  WiFi.begin(ssid, password);
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 10000) delay(200);
  lcd.clear(); lcd.print(WiFi.status() == WL_CONNECTED ? "WiFi OK" : "WiFi fallo");
  delay(1500); lcd.clear(); lcd.print("Esperando tarjeta");
}

void connectMQTT() {
  lcd.clear(); lcd.print("Conectando MQTT");
  while (!mqttClient.connected()) {
    if (mqttClient.connect("arduinoClient", mqttUser, mqttPass)) {
      lcd.clear(); lcd.print("MQTT OK");
      mqttClient.subscribe("acceso/motor");
      mqttClient.subscribe("proyecto/micro/colores");
      mqttClient.subscribe("proyecto/micro/qr");
      mqttClient.subscribe("proyecto/micro/sensores");
    } else {
      delay(1000);
    }
  }
  delay(1000); lcd.clear(); lcd.print("Esperando tarjeta");
}

bool checkCapacidad() {
  httpClient.get(apiPath2);
  int statusCode = httpClient.responseStatusCode();
  String response = httpClient.responseBody();
  StaticJsonDocument<128> doc;
  DeserializationError error = deserializeJson(doc, response);
  if (error) {
    Serial.println("Error JSON");
    return false;
  }

  int bloqueo = doc["bloqueo"];
  Serial.print("Bloqueo: "); Serial.println(bloqueo);
  return (bloqueo == 0); // Si está bloqueado, no se puede abrir
}

// ===================================================================
//                   CALLBACK MQTT CON FILTRO CAPACIDAD
// ===================================================================
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String msg;
  for (unsigned int i = 0; i < length; i++) msg += (char)payload[i];
  Serial.println("MQTT " + String(topic) + " -> " + msg);

  if (strcmp(topic, "proyecto/micro/colores") == 0) {
    DynamicJsonDocument doc(256);
    DeserializationError err = deserializeJson(doc, msg);
    if (!err) {
      String c = doc["color"].as<String>();
      colorValue = (c == "Rosa") ? 3 : (c == "Azul") ? 1 : (c == "Gris") ? 2 : 3;
    }
    Serial.print("colorValue = "); Serial.println(colorValue);
  }

  else if (strcmp(topic, "acceso/motor") == 0) {
    if (msg == "OPEN" && !motorActive) {
      if (checkCapacidad()) { 
        motorActive = true;
        lcd.clear(); lcd.print("Motor ON (OK)");
        sendColorPulse();
      } else {
        lcd.clear(); lcd.print("Capacidad llena");
        lcd.setCursor(0, 1); lcd.print("Motor OFF");
        Serial.println("No se puede abrir: Capacidad llena");
      }
      lastDisplayMillis = millis();
    }

    else if (msg == "CLOSE" && motorActive) {
      motorActive = false;
      lcd.clear(); lcd.print("MQTT: Motor OFF");
      digitalWrite(MOTOR_PIN_1, HIGH);
      digitalWrite(MOTOR_PIN_2, HIGH);
      lastDisplayMillis = millis();

      lcd.clear(); lcd.print("Escanea QR");
      storedColor = 3; storedPeso = 0; lastQR = ""; contador = 0;
    }
  }

  else if (strcmp(topic, "proyecto/micro/qr") == 0) {
    if (contador != 0) return;
    if (msg == "INVALID QR") {
      lcd.clear(); lcd.print("QR invalido,");
      lcd.setCursor(0, 1); lcd.print("lealo de nuevo");
      delay(1500); lcd.clear(); lcd.print("Escanea QR");
      lastDisplayMillis = millis(); return;
    }
    lastQR = msg;
    lcd.clear(); lcd.print("QR valido:");
    lcd.setCursor(0, 1); lcd.print(msg);
    delay(1500);

    if (storedPeso > 0) {
      contador = 1;
      flagLeerColor = true;
    } else {
      contador = 0;
      flagLeerPeso = true;
    }
  }
  else if (strcmp(topic, "proyecto/micro/sensores") == 0) {
    DynamicJsonDocument doc(256);
    DeserializationError err = deserializeJson(doc, msg);
    if (!err && doc.containsKey("peso")) {
      peso = doc["peso"].as<int>();
    } else {
      int idx = msg.indexOf("peso:");
      if (idx >= 0) {
        String val = msg.substring(idx + 5);
        val.trim();
        peso = val.toInt();
      }
    }
    Serial.print("Peso = ");
    Serial.println(peso);
  }
  
  lastQR = msg;                       // QR válido
    lcd.clear();
    lcd.print("QR valido:");
    lcd.setCursor(0, 1);
    lcd.print(msg);
    delay(1500);

    lcd.clear();
    lcd.print("Situa bolsa en");
    lcd.setCursor(0, 1);
    lcd.print("plataforma");
    delay(2000);

    lcd.clear();
    lcd.print("Pulsa boton para");
    lcd.setCursor(0, 1);
    lcd.print("leer peso");
    lcd.write(byte(0));                 // icono reciclaje
    contador = 1;                       // siguiente botón → peso
    delay(500);
    lastDisplayMillis = millis();
  }

// ------------------- RFID login / logout ---------------------------
void handleRFID() {
  if (!mfrc522.PICC_IsNewCardPresent() || !mfrc522.PICC_ReadCardSerial())
    return;

  // Construir UID en hex
  String uidStr;
  for (uint8_t i = 0; i < mfrc522.uid.size; i++) {
    char buf[3];
    sprintf(buf, "%02x", mfrc522.uid.uidByte[i]);
    uidStr += buf;
  }

  // -------- Logout -------------------------------------------------
  if (authorized && uidStr == activeUID) {
    lcd.clear();
    lcd.print("Sesion cerrada");
    mqttClient.publish("acceso/usuario", "LOGOUT");
    authorized  = false;
    activeUID   = "";
    username    = "";
    motorActive = false;
    digitalWrite(MOTOR_PIN_1, HIGH);
    digitalWrite(MOTOR_PIN_2, HIGH);
    delay(1500);
    lcd.clear();
    lcd.print("Esperando tarjeta");
    return;
  }

  // -------- Otra tarjeta con sesión activa ------------------------
  if (authorized && uidStr != activeUID) {
    lcd.clear();
    lcd.print("Ya hay sesion");
    delay(1500);
    lcd.clear();
    lcd.print("Pulse boton");
    return;
  }

  // -------- Consulta a API ----------------------------------------
  String url = String(apiPath) + uidStr;
  Serial.print("GET https://");
  Serial.print(apiHost);
  Serial.println(url);

  lcd.clear();
  lcd.print("Consultando...");
  int status = httpClient.get(url);
  String response = httpClient.responseBody();
  httpClient.stop();

  Serial.print("HTTP status: ");
  Serial.println(status);
  Serial.print("Body: ");
  Serial.println(response);

  DynamicJsonDocument doc(1024);
  DeserializationError err = deserializeJson(doc, response);
  if (err) {
    lcd.clear();
    lcd.print("Error servidor");
    delay(1500);
    lcd.clear();
    lcd.print("Esperando tarjeta");
    return;
  }

  bool remoteOk = doc["authorized"];
  username      = doc["user"]["nombre"].as<String>();

  if (remoteOk) {                       // -- LOGIN --
    authorized = true;
    activeUID  = uidStr;

    lcd.clear();
    lcd.print("Bienvenido ");
    lcd.setCursor(0, 1);
    lcd.print(username);
    mqttClient.publish("acceso/usuario", "LOGIN");
    delay(1500);

    lcd.clear();
    lcd.print("Escanea QR");
    lastDisplayMillis = millis();
  } else {                              // -- NO auth --
    lcd.clear();
    lcd.print("Usuario no auth");
    delay(1500);
    lcd.clear();
    lcd.print("Esperando tarjeta");
  }

  mfrc522.PICC_HaltA();
}

// ------------------------- IRQ del botón ----------------------------
void buttonISR() {
  if (!authorized) return;

  switch (contador) {
    case 0:      // aún esperando QR
      return;

    case 1:      // leer peso
      flagLeerPeso = true;
      contador = 2;
      break;

    case 2:      // leer color
      flagLeerColor = true;
      contador = 3;
      break;

    case 3:      // reciclar
      flagReciclarBolsa = true;
      // contador se reinicia en loop() tras reciclar
      break;
  }
}
