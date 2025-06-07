//Librerías WiFi
#include <WiFi.h>
#include <PubSubClient.h>

//Librerías sensor de peso
#include <HX711.h>
#include <Preferences.h>

//Librerías sensor de temperatura
#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <DHT_U.h>

////////////////////////////////////////////////////////////
//////////////          MQTT          //////////////////////
///////////////////////////////////////////////////////////

// Datos de red
const char* ssid = "vivo V40 SE 5G";
const char* password = "uaz3fhfzdvufkav";

// Datos del broker MQTT
const char* mqtt_server = "172.201.114.19";  // Puedes usar uno local o público
const int mqtt_port = 1883;

// Cliente WiFi y MQTT
WiFiClient espClient;
PubSubClient client(espClient);

///////////////////////////////////////////////////////////
///////////////    SENSOR   DE   PESO   //////////////////
/////////////////////////////////////////////////////////

// Pines para la celda de carga única
#define DT1 32    // Datos de la celda de carga
#define SCK1 33   // Reloj de la celda de carga

// Inicialización de la celda de carga
HX711 celda;

// Pin para el valor analógico de Compresión (si lo sigues usando)
#define PIN_COMPRESION 34 // Pin ADC para leer Compresión

// Pin para el botón de reinicio (tara) de la celda
#define BOTON_REINICIO 27

// Variable para calibración
float escalaCelda = 1.0; // Valor inicial para la escala
bool transmitir = false; // Control de transmisión de datos

// Inicialización de Preferences
Preferences preferences;

// Forward declarations
void cargarCalibracion();
void guardarCalibracion();
void calibrarCelda();
void mostrarCalibracion();
void menu();

///////////////////////////////////////////////////////////////
///////////       SENSOR   DE   TEMPERATURA    ///////////////
/////////////////////////////////////////////////////////////

#define DHTPIN 15       // Pin conectado al DHT11
#define DHTTYPE DHT11   // Tipo de sensor

DHT dht(DHTPIN, DHTTYPE);

// ----------- CONEXIÓN WIFI Y MQTT -----------
void conectarWiFi() {
  Serial.print("Conectando a WiFi...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("Conectado!");
}

void reconectarMQTT() {
  while (!client.connected()) {
    Serial.print("Conectando a MQTT...");
    if (client.connect("ESP32PesoTemp")) {
      Serial.println("conectado!");
    } else {
      Serial.print("falló, rc=");
      Serial.print(client.state());
      Serial.println(" intentando de nuevo en 5s");
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(115200);

  conectarWiFi();

  client.setServer(mqtt_server, mqtt_port);

  setUpSensorTemperatura();
  setUpSensorPeso();
}

void loop() {
  if (!client.connected()) {
    reconectarMQTT();
  }
  client.loop();
  loopSensorTemperatura();
  loopSensorPeso();


}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////            SENSOR     DE     TEMPERATURA             ////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

void setUpSensorTemperatura(){
  dht.begin();
  Serial.println("Sensor DHT11 con ESP32 listo");
}

void loopSensorTemperatura(){
    delay(2000);  // Espera para que el sensor se estabilice

  float temperatura = dht.readTemperature();  // En °C
  float humedad = dht.readHumidity();

  if (isnan(temperatura) || isnan(humedad)) {
    Serial.println("Error leyendo el sensor DHT11");
    return;
  }

  Serial.print("Temperatura: ");
  Serial.print(temperatura);
  Serial.println(" °C");

  Serial.print("Humedad: ");
  Serial.print(humedad);
  Serial.println(" %");

  float hi = calcularHeatIndex(temperatura, humedad);
  evaluarSalida(hi);

  String payload = String("{\"temperatura\":") + temperatura + 
                 ",\"humedad\":" + humedad + 
                 ",\"heatIndex\":" + hi + "}";

  client.publish("proyecto/micro/sensores", payload.c_str());

}

// Fórmula del índice de calor en grados Celsius
float calcularHeatIndex(float T, float RH) {
  float c1 = -8.784694;
  float c2 = 1.61139411;
  float c3 = 2.338548;
  float c4 = -0.14611605;
  float c5 = -0.012308094;
  float c6 = -0.016424828;
  float c7 = 0.002211732;
  float c8 = 0.00072546;
  float c9 = -0.000003582;

  float HI = c1 + c2*T + c3*RH + c4*T*RH + c5*T*T + c6*RH*RH + c7*T*T*RH + c8*T*RH*RH + c9*T*T*RH*RH;
  return HI;
}

void evaluarSalida(float heatIndex) {
  Serial.print("Índice de calor: ");
  Serial.print(heatIndex);
  Serial.println(" °C");

  if (heatIndex < 32) {
    Serial.println("🌿 Está suave. Puedes salir tranquilamente.");
  } else if (heatIndex < 38) {
    Serial.println("🥵 Hace calorcito fuerte. Sal si es necesario, y con agua.");
  } else if (heatIndex < 41) {
    Serial.println("⚠️ Hace un bochorno tremendo. Mejor espera al atardecer.");
  } else {
    Serial.println("🚫 Ni se te ocurra salir. Peligro de golpe de calor.");
  }
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////                        SENSOR      DE      PESO                         /////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

void setUpSensorPeso(){
  celda.begin(DT1, SCK1);

  // Botón con pull-up interno
  pinMode(BOTON_REINICIO, INPUT_PULLUP);

  // Cargar calibración desde memoria
  cargarCalibracion();

  // Mostrar menú de comandos
  menu();
}

void loopSensorPeso(){
  // Leer comandos desde el puerto serie
  if (Serial.available() > 0) {
    String comando = Serial.readStringUntil('\n');

    if (comando == "S") {
      transmitir = true;
      Serial.println("Transmisión iniciada.");
    }
    else if (comando == "P") {
      transmitir = false;
      Serial.println("Transmisión detenida.");
    }
    else if (comando == "C") {
      calibrarCelda();
      guardarCalibracion();
    }
    else if (comando == "V") {
      mostrarCalibracion();
    }
    else if (comando == "M") {
      menu();
    }
  }

  // Reiniciar la tara si se pulsa el botón
  if (digitalRead(BOTON_REINICIO) == LOW) {
    Serial.println("Reiniciando celda...");
    celda.tare();
    delay(1000); // Anti-rebote
  }

  // Enviar datos
  
    if (celda.is_ready()) {
      float peso = celda.get_units(10);        // Promedia 10 lecturas
      int compresion = analogRead(PIN_COMPRESION);

      String mensaje = String("{\"peso\":") + String(peso, 2) +
                 ",\"compresion\":" + compresion + "}";

      Serial.println(mensaje);
      client.publish("proyecto/micro/sensores", mensaje.c_str());


      delay(500);
    } else {
      Serial.println("Esperando que la celda esté lista...");
    }
  
}
// Muestra el menú de comandos
void menu() {
  Serial.println("Comandos disponibles:");
  Serial.println("'S' - Iniciar transmisión.");
  Serial.println("'P' - Detener transmisión.");
  Serial.println("'C' - Calibrar celda.");
  Serial.println("'V' - Ver calibración guardada.");
  Serial.println("'M' - Mostrar este menú.");
}

// Proceso de calibración de la celda
void calibrarCelda() {
  Serial.println("Coloque la celda sin peso y presione Enter.");
  while (Serial.read() != '\n') {}

  celda.tare();

  Serial.println("Ahora coloque un peso conocido y presione Enter.");
  while (Serial.read() != '\n') {}

  Serial.println("Ingrese el valor del peso en gramos:");
  while (!Serial.available()) {}
  float pesoConocido = Serial.parseFloat();

  Serial.println("Calibrando, por favor espere...");
  long lectura = celda.get_units(10);
  float escala = lectura / pesoConocido;
  celda.set_scale(escala);
  escalaCelda = escala;

  Serial.print("Calibración completada. Factor de escala: ");
  Serial.println(escala, 5);
}

// Carga la calibración desde la memoria flash
void cargarCalibracion() {
  preferences.begin("calibraciones", true);
  escalaCelda = preferences.getFloat("escala", 1.0);
  preferences.end();
  celda.set_scale(escalaCelda);
  Serial.println("Calibración cargada.");
}

// Guarda la calibración en la memoria flash
void guardarCalibracion() {
  preferences.begin("calibraciones", false);
  preferences.putFloat("escala", escalaCelda);
  preferences.end();
  Serial.println("Calibración guardada.");
}

// Muestra la calibración actualmente guardada
void mostrarCalibracion() {
  Serial.print("Factor de escala actual: ");
  Serial.println(escalaCelda, 5);
}

