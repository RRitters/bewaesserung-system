#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <time.h>
#include <vector>
#include <math.h>

// --- SIMULATIONS-SCHALTER ---
// Solange kein RS485/Modbus und keine Fühler angeschlossen sind, erzeugt der ESP32 plausible Messwerte.
// Sobald echte Hardware angeschlossen ist, auf false setzen und Modbus-Auslesung aktivieren.
#define SIMULATION_MODUS true

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

struct MesswertDatensatz {
    uint16_t typId;
    uint8_t  modbusAddr;
    float    feuchte;
    float    temperatur;
    float    ec;
    float    ph;
    float    n;
    float    p;
    float    k;
    float    durchflussLpm;
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

    // Starr 7 Stunden (25200 Sekunden) für Thailand (ICT / UTC+7) addieren
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

    int getAktuelleStufe() const {
        return aktuelleStufe;
    }

    // Wird alle 500ms aufgerufen (entspricht einem von 20 Schritten für die 10s Laufzeit)
    void updateRegelung() {
        if (ventilModbusId <= 0) return;

        if (istDurchflussLpm > MAX_DURCHFLUSS) {
            Serial.printf("[ALARM] Ventil Modbus %d: Durchfluss %.1f l/min ueber Max-Grenze (30 l/min)!\n",
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

        // Totband +-0.3 l/min gegen ständiges Nachregeln
        if (istDurchflussLpm < (sollDurchflussLpm - 0.3f) && aktuelleStufe < 20) {
            aktuelleStufe++;
            Serial.printf("Ventil %d oeffnet einen Schritt (Stufe %d/20) | Ist: %.1f / Soll: %.1f l/min\n",
                          ventilModbusId, aktuelleStufe, istDurchflussLpm, sollDurchflussLpm);
        } else if (istDurchflussLpm > (sollDurchflussLpm + 0.3f) && aktuelleStufe > 0) {
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

// --- PLATZ KLASSE ---
class Platz {
public:
    int platzId = 0;
    String platzName;
    std::vector<SensorSlot> sensorSlots;
    std::vector<MesswertDatensatz> aktuelleMesswerte;
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
        aktuelleMesswerte.clear();
        String ts = getFormatiertenZeitstempel();
        Serial.printf("[%s] Lese Sensoren fuer Platz %s (ID: %d)%s\n",
                      ts.c_str(),
                      platzName.c_str(),
                      platzId,
                      SIMULATION_MODUS ? " [SIMULATION]" : "");

        // Sanfte Zeit-Welle für realistische, platzspezifische Messwerte
        float phase = (millis() / 10000.0f) + (platzId * 0.7f);

        for (const auto& slot : sensorSlots) {
            MesswertDatensatz m = {};
            m.typId = slot.typId;
            m.modbusAddr = slot.modbusAddr;

            if (SIMULATION_MODUS) {
                switch (slot.typId) {
                    case TYP_ID_SOIL_2IN1: // 10 cm Boden (ComWinTop TH-S)
                        m.feuchte    = 38.0f + sin(phase) * 4.5f + (random(-20, 21) / 100.0f);
                        m.temperatur = 26.5f + cos(phase) * 1.2f;
                        Serial.printf("  -> TYP 1 (10cm, MB %u): Feuchte=%.1f%%, Temp=%.1fC\n",
                                      slot.modbusAddr, m.feuchte, m.temperatur);
                        break;

                    case TYP_ID_SOIL_7IN1: // 35 cm Boden (ComWinTop NPKPHCTH-S)
                        m.feuchte    = 46.0f + sin(phase * 0.6f) * 3.0f;
                        m.temperatur = 25.0f + cos(phase * 0.6f) * 0.8f;
                        m.ec         = 1.25f + sin(phase) * 0.15f;
                        m.ph         = 6.4f + cos(phase) * 0.2f;
                        m.n          = 42.0f + sin(phase) * 3.0f;
                        m.p          = 18.0f + cos(phase) * 2.0f;
                        m.k          = 55.0f + sin(phase) * 4.0f;
                        Serial.printf("  -> TYP 2 (35cm, MB %u): Feuchte=%.1f%%, Temp=%.1fC, pH=%.1f, EC=%.2f\n",
                                      slot.modbusAddr, m.feuchte, m.temperatur, m.ph, m.ec);
                        break;

                    case TYP_ID_AIR: // Luftfeuchte & Lufttemperatur
                        m.temperatur = 30.2f + sin(phase * 0.5f) * 2.5f;
                        m.feuchte    = 68.0f - sin(phase * 0.5f) * 6.0f;
                        Serial.printf("  -> TYP 3 (Luft, MB %u): Temp=%.1fC, Feuchte=%.1f%%\n",
                                      slot.modbusAddr, m.temperatur, m.feuchte);
                        break;

                    case TYP_ID_FLOW: { // Durchfluss gekoppelt an Ventilstellung (0..20 -> 0..24 l/min)
                        float ventilAnteil = durchflussRegler.getAktuelleStufe() / 20.0f;
                        float rauschen = (ventilAnteil > 0.0f) ? (random(-15, 16) / 100.0f) : 0.0f;
                        m.durchflussLpm = max(0.0f, ventilAnteil * 24.0f + rauschen);
                        durchflussRegler.setIstDurchfluss(m.durchflussLpm);
                        Serial.printf("  -> TYP 4 (Flow, MB %u): %.2f l/min (Ventil Stufe %d/20)\n",
                                      slot.modbusAddr, m.durchflussLpm, durchflussRegler.getAktuelleStufe());
                        break;
                    }

                    default:
                        Serial.printf("  -> Unbekannte TYP_ID %u an Modbus %u\n",
                                      slot.typId, slot.modbusAddr);
                        break;
                }
            } else {
                // Hier folgt später der echte Modbus-RTU-Code (wenn SIMULATION_MODUS == false)
            }

            aktuelleMesswerte.push_back(m);
        }
    }

    void sendeSensorenDaten() {
        if (aktuelleMesswerte.empty() || WiFi.status() != WL_CONNECTED) return;

        JsonDocument doc;
        doc["platzId"] = platzId;
        doc["zeitstempel"] = getFormatiertenZeitstempel();
        doc["simulation"] = SIMULATION_MODUS;

        JsonArray werteArray = doc["werte"].to<JsonArray>();
        for (const auto& m : aktuelleMesswerte) {
            JsonObject obj = werteArray.add<JsonObject>();
            obj["typId"] = m.typId;
            obj["modbusAddr"] = m.modbusAddr;

            if (m.typId == TYP_ID_SOIL_2IN1 || m.typId == TYP_ID_AIR) {
                obj["feuchte"] = round(m.feuchte * 10.0f) / 10.0f;
                obj["temperatur"] = round(m.temperatur * 10.0f) / 10.0f;
            } else if (m.typId == TYP_ID_SOIL_7IN1) {
                obj["feuchte"] = round(m.feuchte * 10.0f) / 10.0f;
                obj["temperatur"] = round(m.temperatur * 10.0f) / 10.0f;
                obj["ec"] = round(m.ec * 100.0f) / 100.0f;
                obj["ph"] = round(m.ph * 10.0f) / 10.0f;
                obj["n"] = round(m.n);
                obj["p"] = round(m.p);
                obj["k"] = round(m.k);
            } else if (m.typId == TYP_ID_FLOW) {
                obj["durchflussLpm"] = round(m.durchflussLpm * 100.0f) / 100.0f;
            }
        }

        String jsonPayload;
        serializeJson(doc, jsonPayload);

        HTTPClient http;
        if (http.begin(serverUrl[DATA])) {
            http.addHeader("Content-Type", "application/json");
            int httpCode = http.POST(jsonPayload);
            if (httpCode > 0) {
                Serial.printf("  [POST /data] Platz %d -> HTTP %d\n", platzId, httpCode);
            } else {
                Serial.printf("  [POST /data] Fehler: %s\n", http.errorToString(httpCode).c_str());
            }
            http.end();
        }
    }
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
    randomSeed(esp_random());

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