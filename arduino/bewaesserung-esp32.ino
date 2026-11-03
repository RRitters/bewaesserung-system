#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
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
    FEHLERFREI = 0,  
    GEKOMMEN = 1,
    GEMELDET = 2,
    QUITTIERT_ABER_AKTIV = 3
};

const char *serverUrl[] = {
    "https://throwback-announcer-upfront.ngrok-free.dev/api/arduino/data",         
    "https://throwback-announcer-upfront.ngrok-free.dev/api/arduino/order",        
    "https://throwback-announcer-upfront.ngrok-free.dev/api/arduino/order/ack",    
    "https://throwback-announcer-upfront.ngrok-free.dev/api/arduino/alert",        
    "https://throwback-announcer-upfront.ngrok-free.dev/api/arduino/alert/ack",    
    "https://throwback-announcer-upfront.ngrok-free.dev/api/arduino/configure",    
    "https://throwback-announcer-upfront.ngrok-free.dev/api/arduino/configure/ack" 
};

const char *rebootUrl = "https://throwback-announcer-upfront.ngrok-free.dev/api/arduino/reboot";

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
const unsigned long VERSUCHS_INTERVALL = 10000; 

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
    StoerungsZustand alarmStatus = FEHLERFREI; 

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

    void ueberpruefeStoerungen() { /* Logik kommt später */ }
    void regleDurchfluss() { /* Logik kommt später */ }
    void leseSensoren() { /* Logik kommt später */ }
    void sendeSensorenDaten() { /* Logik kommt später */ }
}; 

std::vector<Platz> gartenPlaetze;

// --- NETZWERK FUNKTIONEN ---

bool meldeNeustart() {
    Serial.println("Versuche Wiederanlauf an Backend zu melden...");
    WiFiClientSecure *secureClient = new WiFiClientSecure;
    
    if (secureClient) {
        secureClient->setInsecure();     
        secureClient->setTimeout(15000); 
        
        HTTPClient http;
        http.begin(*secureClient, rebootUrl); 
        http.addHeader("Content-Type", "application/json");
        http.addHeader("ngrok-skip-browser-warning", "true"); 
        
        String payload = "{\"status\": \"reboot\", \"grund\": \"power_on\"}";
        int httpCode = http.POST(payload);

        bool erfolg = (httpCode == HTTP_CODE_OK || httpCode == 200 || httpCode == 201);
        if (erfolg) {
            Serial.println("Neustart erfolgreich ans Backend gemeldet!");
        } else {
            Serial.printf("Backend nicht erreichbar. HTTP Code: %d\n", httpCode);
        }
        
        http.end();
        delete secureClient; 
        return erfolg;
    }
    return false;
}

void sendeConfigAck() {
    WiFiClientSecure *secureClient = new WiFiClientSecure;
    if (secureClient) {
        secureClient->setInsecure();
        secureClient->setTimeout(15000);
        
        HTTPClient http;
        http.begin(*secureClient, serverUrl[CONFIGURE_ACK]);
        http.addHeader("Content-Type", "application/json");
        http.addHeader("ngrok-skip-browser-warning", "true");

        String payload = "{\"status\":\"ok\",\"plaetzeKonfiguriert\":" + String(gartenPlaetze.size()) + "}";
        int httpCode = http.POST(payload);
        
        if(httpCode > 0) {
            Serial.printf("Config-ACK gesendet. HTTP Code: %d\n", httpCode);
        } else {
            Serial.printf("Fehler beim Senden des Config-ACK: %d\n", httpCode);
        }
        http.end();
        delete secureClient;
    }
}

bool ladeKonfigurationVomServer() {
    Serial.println("Versuche Konfiguration vom Server zu holen...");
    WiFiClientSecure *secureClient = new WiFiClientSecure;
    bool erfolg = false;

    if (secureClient) {
        secureClient->setInsecure();
        secureClient->setTimeout(15000);
        
        HTTPClient http;
        http.begin(*secureClient, serverUrl[CONFIGURE]);
        http.addHeader("ngrok-skip-browser-warning", "true");
        
        int httpCode = http.GET();

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
        delete secureClient;
    }
    return erfolg;
}

// --- SETUP ---
void setup() {
    Serial.begin(115200);

    WiFi.mode(WIFI_STA);
    WiFi.disconnect(true); 
    delay(1000); 

    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    
    Serial.print("Verbinde mit WiFi ");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nVerbunden!");
}

// --- LOOP ---
void loop() {
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
        return; 
    }

    unsigned long currentMillis = millis();
    
    anzahlPlaetze = gartenPlaetze.size();
    if (anzahlPlaetze == 0) return;

    // Task 1: Ventile
    if (currentMillis - lastVentilTime >= ventilIntervall) {
        for (int i = 0; i < anzahlPlaetze; i++) {
            gartenPlaetze[i].regleDurchfluss();
            gartenPlaetze[i].ueberpruefeStoerungen();
        }
        lastVentilTime = currentMillis;
    }

    // Task 2: Sensoren
    if (currentMillis - lastSensorTime >= sensorIntervall) {
        gartenPlaetze[platzIndex].leseSensoren();
        gartenPlaetze[platzIndex].sendeSensorenDaten();

        platzIndex++;
        if (platzIndex >= anzahlPlaetze) platzIndex = 0;

        lastSensorTime = currentMillis;
    }

    // Task 3: Netzwerk
    if (currentMillis - lastNetzwerkTime >= netzwerkIntervall) {
        if (WiFi.status() == WL_CONNECTED) {
            // ...
        }
        lastNetzwerkTime = currentMillis;
    }
}
