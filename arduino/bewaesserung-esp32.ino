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
    ALERT = 3,       // Für Störungen und Warnungen (POST)
    ALERT_ACK = 4    // Backend bestätigt, dass die Störung gesehen wurde (POST)
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
    "http://192.168.1.45:8080/api/arduino/order/ack"  // [ORDER_ACK]
    "http://192.168.1.45:8080/api/arduino/alert",     // [ALERT]
    "http://192.168.1.45:8080/api/arduino/alert/ack"  // [ALERT_ACK]
};

// --- TIMER VARIABLEN ---
unsigned long lastTime = 0;
unsigned long timerDelay = 5000; // 5 Sekunden Polling-Intervall

// --- KLASSEN DEFINITIONEN ---

class Messwert {
  public:
    float wert;
    unsigned long zeitstempel;

    Messwert(float w) {
      wert = w;
      zeitstempel = millis();
    }
};

class ModbusSensor {
  private:
    int modbusId;
    std::vector<Messwert> verlauf;

  public:
    ModbusSensor(int id) {
      modbusId = id;
    }

    void leseUndSpeichereWert() {
      // Hier kommt später der echte Hardware-Aufruf rein
      float aktuellerWert = 24.5; 
      verlauf.push_back(Messwert(aktuellerWert));
    }

    int getId() { return modbusId; }
};

class Ventil {
  private:
    int pin;
    int aktuellerDurchfluss;

  public:
    Ventil(int p) {
      pin = p;
      pinMode(pin, OUTPUT); 
      schliessen();
    }

    void oeffnen() { setzeDurchfluss(100); }
    void schliessen() { setzeDurchfluss(0); }

    void setzeDurchfluss(int prozent) {
      if(prozent < 0) prozent = 0;
      if(prozent > 100) prozent = 100;
      
      aktuellerDurchfluss = prozent;
      int pwmWert = map(prozent, 0, 100, 0, 255);
      analogWrite(pin, pwmWert); 
    }
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
        Serial.print("JSON Parse Fehler: ");
        Serial.println(error.c_str());
        return;
      }

      name = doc["platzName"].as<String>();
      ventilPin = doc["ventilPin"].as<int>();
      
      JsonArray sensorenArray = doc["modbusIds"].as<JsonArray>();
      for (JsonVariant id : sensorenArray) {
        modbusSensoren.push_back(id.as<int>());
      }

      // Hardware provisorisch initialisieren
      pinMode(ventilPin, OUTPUT);
      digitalWrite(ventilPin, LOW); 

      ausgabe();
    }

    // Diese Funktion wird im schnellen Timer (z.B. alle 200ms) aufgerufen
    void ueberpruefeStoerungen() {
      bool physikalischerFehler = false;
      
      // Beispiel-Logik: Druckschalter am Ventil abgefallen?
      // if (digitalRead(druckSensorPin) == LOW) physikalischerFehler = true;

      // Zustandsautomat
      if (physikalischerFehler) {
        // Fehler tritt neu auf
        if (alarmStatus == OK) {
          alarmStatus = GEKOMMEN;
        }
        // Wenn er auf GEMELDET oder QUITTIERT_ABER_AKTIV steht, 
        // machen wir NICHTS. Er bleibt still.
      } 
      else {
        // Physischer Fehler ist weg!
        if (alarmStatus != OK) {
          alarmStatus = OK;
          // Hier könntest du dir merken, dass du dem Backend ein 
          // "Fehler behoben" (Gegangen-Meldung) schicken möchtest.
        }
      }
    }
    
    void ausgabe() {
      Serial.println("--- Platz konfiguriert ---");
      Serial.println("Name: " + name);
      Serial.println("Ventil an Pin: " + String(ventilPin));
      Serial.print("Zugeordnete Modbus-Sensoren: ");
      for (int i = 0; i < modbusSensoren.size(); i++) {
        Serial.print(String(modbusSensoren[i]) + " ");
      }
      Serial.println("\n--------------------------");
    }
};

// --- GLOBALE OBJEKTE ---
Platz meinGartenPlatz;

// --- SETUP ---
void setup() {
  Serial.begin(115200);

  // WLAN Verbindung
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Verbinde mit WiFi ");
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("\nVerbunden! IP-Adresse: ");
  Serial.println(WiFi.localIP());

  // Simulation: Konfiguration vom Backend erhalten und verarbeiten
  String backendAntwort = "{\"platzName\":\"Tomatenbeet\",\"ventilPin\":5,\"modbusIds\":[10,11,12,13]}";
  meinGartenPlatz.ladeKonfiguration(backendAntwort);
}

unsigned int platzIndex = 0;
unsigned int sensorIntervall = 1000; // 1 Sekunde
// --- LOOP ---
void loop() {
  if ((millis() - lastTime) > timerDelay) {
    
    if (WiFi.status() == WL_CONNECTED) {
      HTTPClient http;

      // Nutzt jetzt sauber das Enum und das URL-Array!
      http.begin(serverUrl[ORDER]);
      
      Serial.println("Sende GET-Request an Backend für neue Befehle...");
      
      int httpResponseCode = http.GET();
      
      if (httpResponseCode > 0) {
        Serial.print("HTTP Status Code: ");
        Serial.println(httpResponseCode);
        
        String payload = http.getString();
        Serial.println("Antwort vom Backend:");
        Serial.println(payload);
        Serial.println("-------------------------");
      }
      else {
        Serial.print("Fehler beim HTTP-Request. Fehlercode: ");
        Serial.println(httpResponseCode);
      }
      
      http.end();
    }
    else {
      Serial.println("WLAN-Verbindung unterbrochen.");
    }
    
    lastTime = millis();
  }
  unsigned long currentMillis = millis();
  unsigned int anzahlPlaetze = gartenPlaetze.size();

  // ---------------------------------------------------------
  // TASK 1: Schnelle Hardware (Ventile regeln, Durchfluss)
  // Läuft z.B. 5x pro Sekunde
  // ---------------------------------------------------------
  if (currentMillis - lastVentilTime >= ventilIntervall) {
    for (auto& [platzId, aktuellerPlatz] : gartenPlaetze) {
      // Reagiert sofort, liest Durchfluss-Ticks, passt PWM an
      aktuellerPlatz.regleDurchfluss(); 
    }
    lastVentilTime = currentMillis;
  }

  // ---------------------------------------------------------
  // TASK 2: Langsame Hardware (Modbus Sensoren abfragen)
  // Läuft alle 5 Sekunden (sequenziell für alle Plätze)
  // Mir wäre hier lieber jede Sekunde nur ein Platz zu lesen und dann gleich den Satz zu senden
  // Die Mittelwertbildung macht dann das Backend
  // ---------------------------------------------------------
  if (currentMillis - lastSensorTime >= sensorIntervall) {
    gartenPlaetze[platzIndex].leseSensoren(); // Liest nur den aktuellen Platz aus
    gartenPlaetze[platzIndex++].sendeSensorenDaten();
    if(platzIndex >= anzahlPlaetze) platzIndex = 0; // Reset, wenn alle Plätze durchlaufen wurden

//    for (auto& [platzId, aktuellerPlatz] : gartenPlaetze) {
//      // Liest in Ruhe die Modbus-Werte aus
//      aktuellerPlatz.leseSensoren(); 
//    }
    
    // Optional: Hier direkt danach die frisch gesammelten Daten aller 
    // Plätze als JSON-Block per POST an serverUrl[DATA] schicken!
    
    lastSensorTime = currentMillis;
  }

  // ---------------------------------------------------------
  // TASK 3: Netzwerk (Neue Befehle abholen)
  // Läuft alle 10 Sekunden
  // ---------------------------------------------------------
  if (currentMillis - lastNetzwerkTime >= netzwerkIntervall) {
    if (WiFi.status() == WL_CONNECTED) {
      // Backend fragen, ob es neue Soll-Werte gibt
    }
    lastNetzwerkTime = currentMillis;
  }
}
