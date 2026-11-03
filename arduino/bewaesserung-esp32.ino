#include <WiFi.h>
#include <HTTPClient.h>
#include <vector>
#include <ArduinoJson.h>

#define WIFI_SSID "Wokwi-GUEST"
#define WIFI_PASSWORD ""

// --- ENUMS & URLS ---
enum connections {
    DATA = 0,
    ORDER = 1,
    ORDER_ACK = 2,
    ALERT = 3,
    ALERT_ACK = 4,
    CONFIGURE = 5,
    CONFIGURE_ACK = 6
};

enum StoerungsZustand {
    FEHLERFREI = 0,  // Umbenannt von OK
    GEKOMMEN = 1,
    GEMELDET = 2,
    QUITTIERT_ABER_AKTIV = 3
};

const char *serverUrl[] = {
    "http://192.168.1.45:8080/api/arduino/data",         // [DATA]
    "http://192.168.1.45:8080/api/arduino/order",        // [ORDER]
    "http://192.168.1.45:8080/api/arduino/order/ack",    // [ORDER_ACK]
    "http://192.168.1.45:8080/api/arduino/alert",        // [ALERT]
    "http://192.168.1.45:8080/api/arduino/alert/ack",    // [ALERT_ACK]
    "http://192.168.1.45:8080/api/arduino/configure",    // [CONFIGURE]
    "http://192.168.1.45:8080/api/arduino/configure/ack" // [CONFIGURE_ACK]
};

const char *rebootUrl = "http://192.168.1.45:8080/api/arduino/reboot";

// --- TIMER VARIABLEN ---
unsigned long lastVentilTime = 0;
unsigned long lastSensorTime = 0;
unsigned long lastNetzwerkTime = 0;

const unsigned long ventilIntervall = 200;     
const unsigned long sensorIntervall = 1000;    
const unsigned long netzwerkIntervall = 10000; 

// --- STATE MACHINE GLOBALS ---
bool neustartGemeldet = false;
bool configGeladen = false;
unsigned long letzterVerbindungsVersuch = 0;
const unsigned long VERSUCHS_INTERVALL = 10000; // Alle 10 Sekunden neu versuchen

unsigned int platzIndex = 0;
unsigned int anzahlPlaetze = 0;

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

class Platz {
public:
    int platzId;
    String platzName;
    std::vector<int> sensorModbusIds;
    int ventilModbusId;
    int ventilLaufzeitMs;
    int sollPosition;
    StoerungsZustand alarmStatus = FEHLERFREI; // Unser neues "OK"

    // 1. JSON laden
    bool ladeAusJson(JsonObject platzJson) {
        platzId = platzJson["platzId"];
        platzName = platzJson["platzName"].as<String>();

        sensorModbusIds.clear();
        JsonArray modbusArray = platzJson["sensorModbusIds"];
        for (int id : modbusArray) {
            sensorModbusIds.push_back(id);
        }

        ventilModbusId = platzJson["ventilModbusId"];
        ventilLaufzeitMs = platzJson["ventilLaufzeitMs"];
        sollPosition = platzJson["sollPosition"];

        Serial.printf("Platz %s (ID: %d) geladen. Ventil-Modbus: %d\n", platzName.c_str(), platzId, ventilModbusId);
        return true;
    }

    // 2. Störungen prüfen
    void ueberpruefeStoerungen() {
        bool physikalischerFehler = false;
        // z.B. Modbus Timeout Check
        if (physikalischerFehler) {
            if (alarmStatus == FEHLERFREI) alarmStatus = GEKOMMEN;
        } else {
            if (alarmStatus != FEHLERFREI) alarmStatus = FEHLERFREI;
        }
    }

    // 3. Platzhalter für Hardware-Aktionen
    void regleDurchfluss() { /* Modbus Relais steuern */ }
    void leseSensoren() { /* Modbus Sensoren abfragen */ }
    void sendeSensorenDaten() { /* HTTP POST an serverUrl[DATA] */ }
}; // <-- GANZ WICHTIG: Das Semikolon hier am Ende der Klasse!

bool meldeNeustart() {
    Serial.println("Versuche Wiederanlauf an Backend zu melden...");
    HTTPClient http;
    http.begin(rebootUrl);
    http.addHeader("Content-Type", "application/json");

    String payload = "{\"status\": \"reboot\", \"grund\": \"power_on\"}";
    int httpCode = http.POST(payload);

    bool erfolg = (httpCode == HTTP_CODE_OK || httpCode == 200);
    if (erfolg) {
        Serial.println("Neustart erfolgreich ans Backend gemeldet!");
    } else {
        Serial.printf("Backend nicht erreichbar. Code: %d\n", httpCode);
    }
    http.end();
    return erfolg;
}

std::vector<Platz> gartenPlaetze;

// --- NETZWERK FUNKTIONEN ---

void sendeConfigAck() {
    HTTPClient http;
    http.begin(serverUrl[CONFIGURE_ACK]);
    http.addHeader("Content-Type", "application/json");

    String payload = "{\"status\":\"ok\",\"plaetzeKonfiguriert\":" + String(gartenPlaetze.size()) + "}";
    int httpCode = http.POST(payload);
    
    if(httpCode > 0) {
        Serial.printf("Config-ACK gesendet. HTTP Code: %d\n", httpCode);
    } else {
        Serial.printf("Fehler beim Senden des Config-ACK: %s\n", http.errorToString(httpCode).c_str());
    }
    http.end();
}

bool ladeKonfigurationVomServer() {
    Serial.println("Versuche Konfiguration vom Server zu holen...");
    HTTPClient http;
    http.begin(serverUrl[CONFIGURE]);
    int httpCode = http.GET();

    bool erfolg = false;
    if (httpCode == HTTP_CODE_OK || httpCode == 200) {
        String payload = http.getString();
        DynamicJsonDocument doc(2048);
        DeserializationError error = deserializeJson(doc, payload);

        if (!error) {
            gartenPlaetze.clear();
            JsonArray array = doc.as<JsonArray>();
            for (JsonObject platzObj : array) {
                Platz p;
                p.ladeAusJson(platzObj);
                gartenPlaetze.push_back(p);
            }
            Serial.printf("Erfolgreich %d Plätze konfiguriert!\n", gartenPlaetze.size());
            sendeConfigAck();
            erfolg = true; 
        } else {
            Serial.printf("JSON Parse Fehler: %s\n", error.c_str());
        }
    } else {
        Serial.printf("Backend Config Fehler. Code: %d\n", httpCode);
    }

    http.end();
    return erfolg;
}

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
    
    // meldeNeustart() entfernt! Wird ab jetzt in loop() handled.
}

// --- LOOP ---
void loop() {
    // --- SCHRITT 1: PRÜFEN OB WIR EINE KONFIGURATION HABEN ---
    if (!configGeladen) {
        if (millis() - letzterVerbindungsVersuch >= VERSUCHS_INTERVALL || letzterVerbindungsVersuch == 0) {
            letzterVerbindungsVersuch = millis(); 

            if (!neustartGemeldet) {
                neustartGemeldet = meldeNeustart();
            }

            if (neustartGemeldet) {
                configGeladen = ladeKonfigurationVomServer();
            }
        }
        return; // Abbruch bis Config da ist
    }

    unsigned long currentMillis = millis();
   
    if (anzahlPlaetze == 0) return;

    // --- TASK 1: Schnelle Hardware (Ventile) ---
    if (currentMillis - lastVentilTime >= ventilIntervall) {
        for (int i = 0; i < anzahlPlaetze; i++) {
            gartenPlaetze[i].regleDurchfluss();
            gartenPlaetze[i].ueberpruefeStoerungen();
        }
        lastVentilTime = currentMillis;
    }

    // --- TASK 2: Langsame Hardware (Sensoren & Senden) ---
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
            // ...
        }
        lastNetzwerkTime = currentMillis;
    }
}
