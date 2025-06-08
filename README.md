
# ♻️ Ecobins – Sistema Inteligente de Reciclaje

**Ecobins** es un proyecto IoT + Web que busca optimizar la gestión de residuos urbanos mediante incentivos al reciclaje y eficiencia logística para reducir el consumo de combustible de camiones de basura.

## 🚀 ¿Qué hace?

* Identifica al usuario mediante tarjeta RFID.
* Clasifica el tipo de residuo usando una IA de visión por color.
* Genera un QR para seguimiento de cada bolsa.
* Pesa la bolsa y calcula puntos para el usuario.
* Decide automáticamente el contenedor correcto.
* Muestra los puntos ganados en tiempo real vía web.
* Informa al basurero si el contenedor está lleno o no.

## 🧱 Tecnologías

* **Hardware**: Arduino Uno R4 Wifi, ESP32, ESP32-CAM, sensores (temperatura, peso), RFID, motores y LCD.
* **Firmware**: Arduino IDE + PlatformIO (Visual Studio Code).
* **Backend**: Java (Vert.x), API REST, integración con MQTT, MySQL.
* **Frontend**: React, Leaflet, MQTT.js.
* **Comunicación**: MQTT en Azure (Mosquitto en VM Linux).

## 🌍 ¿Por qué es útil?

* Fomenta el reciclaje con recompensas.
* Automatiza la separación de residuos.
* Optimiza rutas de recogida según llenado real de contenedores.
* Escalable, modular y listo para smart cities.
