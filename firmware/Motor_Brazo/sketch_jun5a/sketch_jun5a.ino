#include <SPI.h>
#include <l6474.h>

#define pinGiro1    2   // interrupción externa
#define pinGiro2    3
#define SERVO_PIN   9   // PWM software para servo

L6474 myL6474;

volatile bool giroDetected    = false;
volatile byte estadoValue     = 0;
volatile unsigned long lastInterrupt = 0;

// ISR con debounce
void handleGiroISR() {
  unsigned long now = millis();
  if (now - lastInterrupt < 50) return;
  lastInterrupt = now;

  byte b1 = (digitalRead(pinGiro1) == LOW) ? 1 : 0;
  byte b2 = (digitalRead(pinGiro2) == LOW) ? 1 : 0;
  byte est = (b1 << 1) | b2;
  if (est > 0) {
    estadoValue  = est;
    giroDetected = true;
  }
}

// Envía un pulso de servo (1000µs=0°, 2000µs=90°)
void servoPulse(int microsegundos) {
  digitalWrite(SERVO_PIN, HIGH);
  delayMicroseconds(microsegundos);
  digitalWrite(SERVO_PIN, LOW);
  delayMicroseconds(20000 - microsegundos);
}

void setup() {
  Serial.begin(9600);
  SPI.begin();

  pinMode(pinGiro1, INPUT_PULLUP);
  pinMode(pinGiro2, INPUT_PULLUP);
  pinMode(SERVO_PIN, OUTPUT);
  digitalWrite(SERVO_PIN, LOW);

  // --- 1) Inicialización “oficial” + ajuste NEMA 17 ---
  myL6474.Begin(1);
  // myL6474.AttachFlagInterrupt(MyFlagInterruptHandler); // si quieres alarmas

  // Micro-paso 1/16
  myL6474.SelectStepMode(0, L6474_STEP_SEL_1_16);
  // Corriente (= TVAL)
  myL6474.CmdSetParam(0, L6474_TVAL, 54);

  // Perfil inicial de velocidad (puedes cambiar después en “evento”)
  myL6474.SetMaxSpeed(0, 1600);
  myL6474.SetMinSpeed(0, 200);
  myL6474.SetAcceleration(0, 1000);
  myL6474.SetDeceleration(0, 1000);

  // --- 2) Interrupciones externas para “giro” ---
  attachInterrupt(digitalPinToInterrupt(pinGiro1), handleGiroISR, FALLING);
  attachInterrupt(digitalPinToInterrupt(pinGiro2), handleGiroISR, FALLING);
}

void loop() {
  if (giroDetected) {
    //  Desactiva interrupciones para evitar “rebotes” durante la rutina
    detachInterrupt(digitalPinToInterrupt(pinGiro1));
    detachInterrupt(digitalPinToInterrupt(pinGiro2));

    byte estado = estadoValue;
    giroDetected = false;
    estadoValue  = 0;
    Serial.print("Estado detectado: ");
    Serial.println(estado);

    if (estado == 1) {
      // ── Ejemplo de velocidad dinámica “al estilo oficial” ──
      // 1) Run hacia adelante (min speed = 1000)
        /* ‒ Mueve “una vuelta adelante” (3200 µ-pasos) ‒ */
      Serial.println(F("COLOR AZUL"));
      myL6474.Move(0, FORWARD, 800);
      myL6474.WaitWhileActive(0);

      delay(1000);

      // 5) Mantén servo a 90º un segundo
      unsigned long startServo = millis();
      while (millis() - startServo < 1000) {
        servoPulse(2000);
      }

      /* ‒ Devuelve a posición 0 ‒ */
      Serial.println(F("DEPOSITADO"));
      myL6474.GoTo(0, -200);
      myL6474.WaitWhileActive(0);

      delay(1000);
    }
    else if (estado == 2) {
      // ── Variante: primero hacia atrás, luego servo, luego hacia adelante ──
      Serial.println(F("COLOR GRIS"));
      myL6474.Move(0, BACKWARD, 800);
      myL6474.WaitWhileActive(0);

      delay(1000);

      // 5) Mantén servo a 90º un segundo
      unsigned long startServo = millis();
      while (millis() - startServo < 1000) {
        servoPulse(2000);
      }

      /* ‒ Devuelve a posición 0 ‒ */
      Serial.println(F("DEPOSITADO, VOLVIENDO"));
      myL6474.GoTo(0, -200);
      myL6474.WaitWhileActive(0);

      delay(1000);
    }
    else {
      // 5) Mantén servo a 90º un segundo
       // ── Variante: primero hacia atrás, luego servo, luego hacia adelante ──
      Serial.println(F("COLOR ROSA"));
      myL6474.Move(0, BACKWARD, 0);
      myL6474.WaitWhileActive(0);

      delay(1);

      // 5) Mantén servo a 90º un segundo
      unsigned long startServo = millis();
      while (millis() - startServo < 1000) {
        servoPulse(2000);
      }

      /* ‒ Devuelve a posición 0 ‒ */
      Serial.println(F("DEPOSITADO, VOLVIENDO"));
      myL6474.GoTo(0, 0);
      myL6474.WaitWhileActive(0);

      delay(1);
    }

    // Espera a que el pin vuelva a HIGH (liberar botón) para no retrigar
    while (digitalRead(pinGiro1) == LOW || digitalRead(pinGiro2) == LOW) {
      delay(10);
    }

    // Breve retardo y reactiva interrupciones
    delay(50);
    attachInterrupt(digitalPinToInterrupt(pinGiro1), handleGiroISR, FALLING);
    attachInterrupt(digitalPinToInterrupt(pinGiro2), handleGiroISR, FALLING);
  }

  delay(10);
}

// (Opcional) Manejo de FLAG interno:
void MyFlagInterruptHandler(void) {
  uint16_t status = myL6474.CmdGetStatus(0);
  if ((status & L6474_STATUS_OCD) == 0) {
    Serial.println(F("¡Overcurrent!"));
  }
  // ... chequea otros flags si quieres ...
} 