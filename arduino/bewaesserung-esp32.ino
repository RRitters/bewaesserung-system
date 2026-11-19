#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <time.h>
#include <vector>

// --- WLAN DATEN ---
const char* WIFI_SSID = "Nathamon2508";
const char* WIFI_PASSWORD = "0611546046";

// --- SERVER & ENDPUNKTE ---
const char* SERVER_IP = "192.168.1.45";
const int   SERVER_PORT = 8080;
String serverBaseUrl = "http://" + String(SERVER_IP) + ":" + String(SERVER_PORT);

// Index-Zuordnung für serverUrl[]
enum ApiEndpoints {
    DATA = 0,
    ORDER = 1,
    ORDER_ACK = 2,
    ALERT = 3,
    ALERT_ACK = 4,
    CONFIGURE = 5,
    CONFIGURE_ACK = 6
};

String serverUrl[] = {
    serverBaseUrl + "/api/arduino/data",         // 0
    serverBaseUrl + "/api/arduino/order",        // 1
    serverBaseUrl + "/api/arduino/order/ack",    // 2
    serverBaseUrl + "/api/arduino/alert",        // 3
    serverBaseUrl + "/api/arduino/alert/ack",    // 4
    serverBaseUrl + "/api/arduino/configure",    // 5
    serverBaseUrl + "/api/arduino/configure/ack" // 6
};

String piUrl = serverBaseUrl + "/api/arduino/time";

// --- SENSOR-TYPEN & ADRESSPLAN (1..19 Sensoren, >=20 Ausgaben/Relais) ---
enum SensorTypId : uint16_t {
    TYP_ID_SOIL_2IN1 = 1, // ComWinTop TH-S (10 cm)
    TYP_ID_SOIL_7IN1 = 2, // ComWinTop NPKPHCTH-S (35 cm)
    TYP_ID_AIR       = 3, // Luftfeuchte & Lufttemperatur
    TYP_ID_FLOW      = 4  // Durchfluss / Impulszähler
};

struct SensorSlot {
    uint16_t typId;
    uint8_t  modbusAddr;
};

// --- ZEIT & NTP VARIABLEN ---
const long  gmtOffset_sec = 25200;      // UTC +7 für Thailand (7 * 3600)
const int   daylightOffset_sec = 0;
const char* ntpServer1 = "pool.ntp.org";
const char* ntpServer2 = "time.nist.gov";

// --- STATUS & STEUER-VARIABLEN ---
bool configGeladen = false;
bool neustartGemeldet = false;
unsigned long letzterVerbindungsVersuch = 0;
const unsigned long VERSUCHS_INTERVALL = 10000;

enum StoerungsZustand {
    FEHLERFREI = 0,
    WARNUNG = 1,
    ALARM = 2
};

// Task-Variablen für den Loop
unsigned long lastVentilTime = 0;
const unsigned long ventilIntervall = 500; // 500 ms Takt für die Ventilsteuerung (20 Stufen)
unsigned long lastSensorTime = 0;
const unsigned long sensorIntervall = 2000;
unsigned long lastNetzwerkTime = 0;
const unsigned long netzwerkIntervall = 5000;

int anzahlPlaetze = 0;
int platzIndex = 0;

// --- ZEIT FUNKTIONEN ---
String getFormatiertenZeitstempel() {
    struct timeval tv;
    gettimeofday(&tv, NULL);

    // Wir addieren hier starr 7 Stunden (25200 Sekunden) für Thailand (ICT / UTC+7) drauf,
    // damit der ESP32 unabhängig von der UTC-Systemzeit die echte Ortszeit anzeigt.
    time_t nowSecs = tv.tv_sec + (7 * 3600);

    struct tm timeinfo;
    gmtime_r(&nowSecs, &timeinfo);

    int millisec = tv.tv_usec / 1000;

    char buffer[64];
    snprintf(buffer, sizeof(buffer), "%04d.%02d.%02d %02d:%02d:%02d.%03d",
             timeinfo.tm_year + 1900,
             timeinfo.tm_mon + 1,
             timeinfo.tm_mday,
             timeinfo.tm_hour,
             timeinfo.tm_min,
             timeinfo.tm_sec,
             millisec);

    return String(buffer);
}

bool holeZeitVomRaspberry() {
    HTTPClient http;
    http.begin(piUrl);
    int httpResponseCode = http.GET();

    bool erfolg = false;
    if (httpResponseCode > 0) {
        String payload = http.getString();
        JsonDocument doc;
        DeserializationError error = deserializeJson(doc, payload);

        if (!error) {
            unsigned long epochTime = doc["epochSecond"] | 0UL;
            if (epochTime > 0) {
                struct timeval tv;
                tv.tv_sec = epochTime;
                tv.tv_usec = 0;
                settimeofday(&tv, NULL);
                erfolg = true;
            }
        }
    }
    http.end();
    return erfolg;
}

// --- DURCHFLUSSSTEUERUNGS-KLASSE ---
class Durchflusssteuerung {
private:
    int ventilModbusId;
    float sollDurchflussLpm;
    float istDurchflussLpm;
    float minSprenklerLpm;
    const float MAX_DURCHFLUSS = 30.0f; // Obergrenze (Leckageschutz)
    int aktuelleStufe = 0;              // 0 (zu) bis 20 (voll offen)

public:
    Durchflusssteuerung() {
        ventilModbusId = 0;
        sollDurchflussLpm = 0.0f;
        istDurchflussLpm = 0.0f;
        minSprenklerLpm = 6.0f;
    }

    void initialisiere(int vId, float minLpm) {
        ventilModbusId = vId;
        minSprenklerLpm = minLpm;
        aktuelleStufe = 0;
    }

    void setSollDurchfluss(float soll) {
        sollDurchflussLpm = soll;
    }

    void setIstDurchfluss(float ist) {
        istDurchflussLpm = ist;
    }

    // Wird alle 500ms aufgerufen (entspricht einem von 20 Schritten für die 10s Laufzeit)
    void updateRegelung() {
        if (ventilModbusId <= 0) return;

        if (istDurchflussLpm > MAX_DURCHFLUSS) {
            Serial.printf("[ALARM] Ventil Modbus %d: Durchfluss mit %.1f l/min ueber Max-Grenze (30 l/min)!\n",
                          ventilModbusId, istDurchflussLpm);
            schliesseNotfall();
            return;
        }

        if (sollDurchflussLpm <= 0.0f) {
            if (aktuelleStufe > 0) {
                aktuelleStufe--;
                Serial.printf("Ventil %d schliesst weiter (Stufe %d/20)...\n", ventilModbusId, aktuelleStufe);
            }
            return;
        }

        if (istDurchflussLpm < sollDurchflussLpm && aktuelleStufe < 20) {
            aktuelleStufe++;
            Serial.printf("Ventil %d oeffnet einen Schritt (Stufe %d/20) | Ist: %.1f / Soll: %.1f l/min\n",
                          ventilModbusId, aktuelleStufe, istDurchflussLpm, sollDurchflussLpm);
        } else if (istDurchflussLpm > sollDurchflussLpm && aktuelleStufe > 0) {
            aktuelleStufe--;
            Serial.printf("Ventil %d drosselt einen Schritt (Stufe %d/20) | Ist: %.1f / Soll: %.1f l/min\n",
                          ventilModbusId, aktuelleStufe, istDurchflussLpm, sollDurchflussLpm);
        }
    }

private:
    void schliesseNotfall() {
        aktuelleStufe = 0;
        Serial.printf("[NOTFALL] Ventil Modbus %d sofort geschlossen (Stufe 0/20).\n", ventilModbusId);
    }
};

// --- KLASSEN DEFINITIONEN ---
class Platz {
public:
    int platzId = 0;
    String platzName;
    std::vector<SensorSlot> sensorSlots;
    int ventilModbusId = 0;
    int ventilLaufzeitMs = 0;
    float sollDurchflussLpm = 0.0f;

    Durchflusssteuerung durchflussRegler;
    StoerungsZustand alarmStatus = FEHLERFREI;

    bool ladeAusJson(JsonObject platzJson) {
        platzId = platzJson["platzId"] | 0;
        platzName = platzJson["platzName"] | "";

        sensorSlots.clear();
        JsonArray slotsArray = platzJson["slots"].as<JsonArray>();
        if (!slotsArray.isNull()) {
            for (JsonObject slotObj : slotsArray) {
                uint16_t typId = slotObj["typId"] | 0;
                uint8_t addr   = slotObj["modbusAddr"] | 0;

                if (typId > 0 && addr >= 1 && addr <= 19) {
                    sensorSlots.push_back({typId, addr});
                } else if (addr > 0) {
                    Serial.printf("[WARN] Platz %d: Sensor-Modbus %u ausserhalb 1..19 ignoriert\n",
                                  platzId, addr);
                }
            }
        }

        JsonObject ventilObj = platzJson["ventil"].as<JsonObject>();
        float minLpm = 6.0f;
        if (!ventilObj.isNull()) {
            ventilModbusId    = ventilObj["modbusId"] | 0;
            ventilLaufzeitMs  = ventilObj["laufzeitMs"] | 0;
            sollDurchflussLpm = ventilObj["sollDurchflussLpm"] | 0.0f;
            minLpm            = ventilObj["minDurchflussLpm"] | 6.0f;
        } else {
            // Fallback auf älteres flaches JSON-Format
            ventilModbusId    = platzJson["ventilModbusId"] | 0;
            ventilLaufzeitMs  = platzJson["ventilLaufzeitMs"] | 0;
            sollDurchflussLpm = platzJson["sollPosition"] | 0.0f;
        }

        if (ventilModbusId > 0 && ventilModbusId < 20) {
            Serial.printf("[WARN] Platz %d: Ventil-Modbus %d liegt unter 20 (Ausgaben ab 20 geplant)\n",
                          platzId, ventilModbusId);
        }

        durchflussRegler.initialisiere(ventilModbusId, minLpm);
        durchflussRegler.setSollDurchfluss(sollDurchflussLpm);

        Serial.printf("Platz %s (ID: %d) geladen | Slots: %d | Ventil-Modbus: %d\n",
                      platzName.c_str(), platzId, (int)sensorSlots.size(), ventilModbusId);
        return true;
    }

    void ueberpruefeStoerungen() { /* Logik folgt */ }

    void regleDurchfluss() {
        durchflussRegler.updateRegelung();
    }

    void leseSensoren() {
        Serial.printf("[%s] Lese Sensoren fuer Platz %s (ID: %d)\n",
                      getFormatiertenZeitstempel().c_str(),
                      platzName.c_str(),
                      platzId);

        for (const auto& slot : sensorSlots) {
            switch (slot.typId) {
                case TYP_ID_SOIL_2IN1:
                    // 2-in-1 (10 cm): Register-Offsets für Feuchte & Temperatur pro Modell fest verdrahtet
                    Serial.printf("  -> TYP 1 (2-in-1 10cm) an Modbus %u\n", slot.modbusAddr);
                    break;

                case TYP_ID_SOIL_7IN1:
                    // 7-in-1 (35 cm): Register-Offsets für Feuchte, Temp, EC, pH, N, P, K fest verdrahtet
                    Serial.printf("  -> TYP 2 (7-in-1 35cm) an Modbus %u\n", slot.modbusAddr);
                    break;

                case TYP_ID_AIR:
                    // Luft: Luftfeuchte & Lufttemperatur an slot.modbusAddr auslesen
                    Serial.printf("  -> TYP 3 (Luft) an Modbus %u\n", slot.modbusAddr);
                    break;

                case TYP_ID_FLOW:
                    // Durchfluss: Wert an slot.modbusAddr auslesen & an durchflussRegler übergeben
                    Serial.printf("  -> TYP 4 (Durchfluss) an Modbus %u\n", slot.modbusAddr);
                    break;

                default:
                    // Plan B für zukünftige neue TYP_IDs
                    Serial.printf("  -> Unbekannte TYP_ID %u an Modbus %u\n", slot.typId, slot.modbusAddr);
                    break;
            }
        }
    }

    void sendeSensorenDaten() { /* Logik folgt */ }
};

std::vector<Platz> gartenPlaetze;

// --- NETZWERK FUNKTIONEN ---
bool meldeNeustart() {
    Serial.println("Versuche Wiederanlauf an Backend zu melden...");
    HTTPClient http;
    if (http.begin(serverBaseUrl + "/api/arduino/reboot")) {
        http.addHeader("Content-Type", "application/json");
        String payload = "{\"status\": \"reboot\", \"grund\": \"power_on\"}";
        int httpCode = http.POST(payload);
        bool erfolg = (httpCode == HTTP_CODE_OK || httpCode == 200 || httpCode == 201);
        if (erfolg) Serial.println("Neustart erfolgreich ans Backend gemeldet!");
        http.end();
        return erfolg;
    }
    return false;
}

void sendeConfigAck() {
    HTTPClient http;
    if (http.begin(serverUrl[CONFIGURE_ACK])) {
        http.addHeader("Content-Type", "application/json");
        String payload = "{\"status\":\"ok\",\"plaetzeKonfiguriert\":" + String(gartenPlaetze.size()) + "}";
        int httpCode = http.POST(payload);

        if (httpCode > 0) {
            Serial.printf("Config-ACK gesendet. HTTP Code: %d\n", httpCode);
        } else {
            Serial.printf("Config-ACK Fehler: %s\n", http.errorToString(httpCode).c_str());
        }

        http.end();
    }
}

bool ladeKonfigurationVomServer() {
    Serial.println("Versuche Konfiguration vom Server zu holen...");
    HTTPClient http;
    if (!http.begin(serverUrl[CONFIGURE])) return false;

    int httpCode = http.GET();
    bool erfolg = false;
    if (httpCode == HTTP_CODE_OK || httpCode == 200) {
        String payload = http.getString();
        JsonDocument doc;
        DeserializationError error = deserializeJson(doc, payload);

        if (!error) {
            gartenPlaetze.clear();
            JsonArray array = doc.as<JsonArray>();
            for (JsonObject platzObj : array) {
                Platz p;
                p.ladeAusJson(platzObj);
                gartenPlaetze.push_back(p);
            }
            Serial.printf("Erfolgreich %d Plaetze konfiguriert!\n", (int)gartenPlaetze.size());
            sendeConfigAck();
            erfolg = true;
        } else {
            Serial.printf("JSON Fehler bei Konfiguration: %s\n", error.c_str());
        }
    } else {
        Serial.printf("Konfiguration HTTP Fehler: %d\n", httpCode);
    }
    http.end();
    return erfolg;
}

// --- SETUP ---
void setup() {
    Serial.begin(115200);
    delay(1000);

    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    Serial.print("Verbinde mit WiFi ");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nVerbunden!");
    Serial.print("IP-Adresse: ");
    Serial.println(WiFi.localIP());

    Serial.println("Versuche Zeit vom lokalen Raspberry Pi zu holen...");
    if (holeZeitVomRaspberry()) {
        Serial.println("Uhrzeit via Raspberry Pi synchronisiert: " + getFormatiertenZeitstempel());
    } else {
        Serial.println("Raspberry Pi nicht erreichbar. Fallback NTP...");
        configTime(gmtOffset_sec, daylightOffset_sec, ntpServer1, ntpServer2);
    }
}

// --- LOOP ---
void loop() {
    if (!configGeladen) {
        if (millis() - letzterVerbindungsVersuch >= VERSUCHS_INTERVALL || letzterVerbindungsVersuch == 0) {
            letzterVerbindungsVersuch = millis();

            if (!neustartGemeldet) {
                neustartGemeldet = meldeNeustart();
                return;
            }

            if (neustartGemeldet) {
                configGeladen = ladeKonfigurationVomServer();
                if (configGeladen) {
                    Serial.println(">>> WECHSEL IN DEN REGULAEREN ARBEITS-LOOP SUCCESSFUL <<<");
                }
            }
        }
        return;
    }

    unsigned long currentMillis = millis();
    anzahlPlaetze = gartenPlaetze.size();
    if (anzahlPlaetze == 0) return;

    // Task 1: Ventile & Durchflusssteuerung (im 500ms Takt)
    if (currentMillis - lastVentilTime >= ventilIntervall) {
        for (int i = 0; i < anzahlPlaetze; i++) {
            gartenPlaetze[i].regleDurchfluss();
            gartenPlaetze[i].ueberpruefeStoerungen();
        }
        lastVentilTime = currentMillis;
    }

    // Task 2: Sensoren (im 2-Sekunden Takt)
    if (currentMillis - lastSensorTime >= sensorIntervall) {
        gartenPlaetze[platzIndex].leseSensoren();
        gartenPlaetze[platzIndex].sendeSensorenDaten();

        platzIndex++;
        if (platzIndex >= anzahlPlaetze) platzIndex = 0;

        lastSensorTime = currentMillis;
    }
}