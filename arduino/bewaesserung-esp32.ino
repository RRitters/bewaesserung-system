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
    gmtime_r(&nowSecs, &timeinfo); // gmtime statt localtime, da der Offset schon eingerechnet ist

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
            unsigned long epochTime = doc["epochSecond"];
            struct timeval tv;
            tv.tv_sec = epochTime;
            tv.tv_usec = 0;
            settimeofday(&tv, NULL);
            erfolg = true;
        }
    }
    http.end();
    return erfolg;
}

// --- NEUE DURCHFLUSSSTEUERUNGS-KLASSE ---
class Durchflusssteuerung {
private:
    int ventilModbusId;
    float sollDurchflussLpm;
    float istDurchflussLpm;
    float minSprenklerLpm;
    const float MAX_DURCHFLUSS = 30.0; // Obergrenze (Leckageschutz)
    int aktuelleStufe = 0;             // 0 (zu) bis 20 (voll offen)

public:
    Durchflusssteuerung() {
        ventilModbusId = 0;
        sollDurchflussLpm = 0.0;
        istDurchflussLpm = 0.0;
        minSprenklerLpm = 5.0; // Standard-Minimum für Sprenkler
    }

    void initialisiere(int vId, float minLpm) {
        ventilModbusId = vId;
        minSprenklerLpm = minLpm;
    }

    void setSollDurchfluss(float soll) {
        sollDurchflussLpm = soll;
    }

    void setIstDurchfluss(float ist) {
        istDurchflussLpm = ist;
    }

    // Wird alle 500ms aufgerufen (entspricht einem von 20 Schritten für die 10s Laufzeit)
    void updateRegelung() {
        if (istDurchflussLpm > MAX_DURCHFLUSS) {
            Serial.printf("[ALARM] Ventil Modbus %d: Durchfluss mit %.1f l/min über Max-Grenze (30 l/min)!\n",
                          ventilModbusId, istDurchflussLpm);
            schliesseNotfall();
            return;
        }

        if (sollDurchflussLpm <= 0.0) {
            if (aktuelleStufe > 0) {
                aktuelleStufe--;
                // Modbus Befehl zum schrittweisen Schließen senden
                Serial.printf("Ventil %d schließt weiter (Stufe %d/20)...\n", ventilModbusId, aktuelleStufe);
            }
            return;
        }

        // Regelkreis-Anpassung im 500ms-Takt
        if (istDurchflussLpm < sollDurchflussLpm && aktuelleStufe < 20) {
            aktuelleStufe++;
            Serial.printf("Ventil %d öffnet einen Schritt (Stufe %d/20) | Ist: %.1f / Soll: %.1f l/min\n",
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
        // Sofort-Stopp Befehl an Ventil senden
    }
};

// --- KLASSEN DEFINITIONEN ---
class Platz {
public:
    int platzId;
    String platzName;
    std::vector<int> sensorModbusIds;
    int ventilModbusId;
    int ventilLaufzeitMs;
    int sollPosition;

    // Das Steuerungsobjekt direkt als Member
    Durchflusssteuerung durchflussRegler;

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

        // Durchflussregler mit Ventil-ID und einem angenommenen Sprenkler-Minimum initialisieren
        durchflussRegler.initialisiere(ventilModbusId, 6.0); // z.B. 6 l/min Minimum
        durchflussRegler.setSollDurchfluss((float)sollPosition);

        Serial.printf("Platz %s (ID: %d) geladen. Ventil-Modbus: %d\n", platzName.c_str(), platzId, ventilModbusId);
        return true;
    }

    void ueberpruefeStoerungen() { /* Logik folgt */ }

    void regleDurchfluss() {
        // Wird alle 500 ms aufgerufen
        durchflussRegler.updateRegelung();
    }

    void leseSensoren() {
        Serial.printf("[%s] Lese Sensoren fuer Platz %s (ID: %d)\n",
                      getFormatiertenZeitstempel().c_str(),
                      platzName.c_str(),
                      platzId);
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
            Serial.printf("Erfolgreich %d Plätze konfiguriert!\n", gartenPlaetze.size());
            sendeConfigAck();
            erfolg = true;
        }
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
                    Serial.println(">>> WECHSEL IN DEN REGULÄREN ARBEITS-LOOP SUCCESSFUL <<<");
                }
            }
        }
        return;
    }

    unsigned long currentMillis = millis();
    anzahlPlaetze = gartenPlaetze.size();
    if (anzahlPlaetze == 0) return;

    // Task 1: Ventile & Durchflusssteuerung (jetzt im 500ms Takt)
    if (currentMillis - lastVentilTime >= ventilIntervall) {
        for (int i = 0; i < anzahlPlaetze; i++) {
            gartenPlaetze[i].regleDurchfluss();
            gartenPlaetze[i].ueberpruefeStoerungen();
        }
        lastVentilTime = currentMillis;
    }

    // Task 2: Sensoren (bleibt im 2-Sekunden Takt)
    if (currentMillis - lastSensorTime >= sensorIntervall) {
        gartenPlaetze[platzIndex].leseSensoren();
        gartenPlaetze[platzIndex].sendeSensorenDaten();

        platzIndex++;
        if (platzIndex >= anzahlPlaetze) platzIndex = 0;

        lastSensorTime = currentMillis;
    }
}