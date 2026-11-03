#include <WiFi.h>
#include <HTTPClient.h>
#include <vector>
#include <ArduinoJson.h>

#define WIFI_SSID "Wokwi-GUEST"
#define WIFI_PASSWORD ""

// --- URL KONFIGURATION ---
enum connections {
    DATA = 0,
    ORDER = 1,
    ORDER_ACK = 2,
    ALERT = 3,       
    ALERT_ACK = 4    
};

enum StoerungsZustand {
  OK = 0,
  GEKOMMEN = 1,
  GEMELDET = 2,
  QUITTIERT_ABER_AKTIV = 3
};

const char* serverUrl[] = {
    "http://192.168.1.45:8080/api/arduino/data",      // [DATA]
    "http://192.168.1.45:8080/api/arduino/order",     // [ORDER]
    "http://192.168.1.45:8080/api/arduino/order/ack", // [ORDER_ACK] (Komma fehlte!)
    "http://192.168.1.45:8080/api/arduino/alert",     // [ALERT]
    "http://192.168.1.45:8080/api/arduino/alert/ack"  // [ALERT_ACK]
};

// --- TIMER VARIABLEN ---
unsigned long lastVentilTime = 0;
unsigned long lastSensorTime = 0;
unsigned long lastNetzwerkTime = 0;

const unsigned long ventilIntervall = 200;    // 200 ms
const unsigned long sensorIntervall = 1000;   // 1 Sekunde
const unsigned long netzwerkIntervall = 10000;// 10 Sekunden

// --- KLASSEN DEFINITIONEN ---
class Messwert {
  public:
    float wert;
    unsigned long zeitstempel;
    Messwert(float w) { wert = w; zeitstempel = millis(); }
};

class Platz {
  public:
    String name;
    int ventilPin;
    std::vector<int> modbusSensoren; 
    int id;
    StoerungsZustand alarmStatus = OK;    

    void ladeKonfiguration(String jsonAntwort) {
      JsonDocument doc; 
      DeserializationError error = deserializeJson(doc, jsonAntwort);
      if (error) {
        Serial.println("JSON Parse Fehler!");
        return;
      }

      name = doc["platzName"].as<String>();
      ventilPin = doc["ventilPin"].as<int>();
      
      JsonArray sensorenArray = doc["modbusIds"].as<JsonArray>();
      for (JsonVariant sensorId : sensorenArray) {
        modbusSensoren.push_back(sensorId.as<int>());
      }
      pinMode(ventilPin, OUTPUT);
      digitalWrite(ventilPin, LOW); 
    }

    void ueberpruefeStoerungen() {
      bool physikalischerFehler = false; 
      // if (digitalRead(pin) == LOW) physikalischerFehler = true;

      if (physikalischerFehler) {
        if (alarmStatus == OK) alarmStatus = GEKOMMEN;
      } else {
        if (alarmStatus != OK) alarmStatus = OK;
      }
    }
    
    // Platzhalter für deine Logik
    void regleDurchfluss() { /* PWM anpassen */ }
    void leseSensoren() { /* Modbus abfragen */ }
    void sendeSensorenDaten() { /* HTTP POST an serverUrl[DATA] */ }
};

// --- GLOBALE OBJEKTE ---
// Vector statt Map, damit der Zugriff über index (0, 1, 2) sicher funktioniert
std::vector<Platz> gartenPlaetze;
unsigned int platzIndex = 0;

// --- SETUP ---
void setup() {
  Serial.begin(115200);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Verbinde mit WiFi ");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nVerbunden!");

  // Simulation: Ein paar Plätze anlegen, damit der Vector nicht leer ist
  Platz beet1;
  beet1.ladeKonfiguration("{\"platzName\":\"Tomaten\",\"ventilPin\":5,\"modbusIds\":[10]}");
  gartenPlaetze.push_back(beet1);

  Platz beet2;
  beet2.ladeKonfiguration("{\"platzName\":\"Gurken\",\"ventilPin\":6,\"modbusIds\":[11]}");
  gartenPlaetze.push_back(beet2);
}

// --- LOOP ---
void loop() {
  unsigned long currentMillis = millis();
  unsigned int anzahlPlaetze = gartenPlaetze.size();

  // Sicherheitsabbruch, falls keine Plätze konfiguriert sind
  if (anzahlPlaetze == 0) return; 

  // --- TASK 1: Schnelle Hardware (Ventile) ---
  if (currentMillis - lastVentilTime >= ventilIntervall) {
    for (int i = 0; i < anzahlPlaetze; i++) {
      gartenPlaetze[i].regleDurchfluss(); 
      gartenPlaetze[i].ueberpruefeStoerungen();
    }
    lastVentilTime = currentMillis;
  }

  // --- TASK 2: Langsame Hardware (Sensoren & Senden, Round-Robin) ---
  if (currentMillis - lastSensorTime >= sensorIntervall) {
    gartenPlaetze[platzIndex].leseSensoren(); 
    gartenPlaetze[platzIndex].sendeSensorenDaten();
    
    platzIndex++;
    if (platzIndex >= anzahlPlaetze) platzIndex = 0; 
    
    lastSensorTime = currentMillis;
  }

  // --- TASK 3: Netzwerk (Neue Befehle abholen) ---
  if (currentMillis - lastNetzwerkTime >= netzwerkIntervall) {
    if (WiFi.status() == WL_CONNECTED) {
      HTTPClient http;
      http.begin(serverUrl[ORDER]);
      int httpResponseCode = http.GET();
      if (httpResponseCode > 0) {
         // Befehle verarbeiten
      }
      http.end();
    }
    lastNetzwerkTime = currentMillis;
  }
}
