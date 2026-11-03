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
const int   daylightOffset_sec = 0;     // Kein Daylight Saving in Thailand
const char* ntpServer1 = "pool.ntp.org";
const char* ntpServer2 = "time.nist.gov";

// --- STATUS & STEUER-VARIABLEN ---
bool configGeladen = false;
bool neustartGemeldet = false;
unsigned long letzterVerbindungsVersuch = 0;
const unsigned long VERSUCHS_INTERVALL = 10000; // 10 Sekunden

// Zustand für Störungen (Dummy-Enum, falls noch nicht definiert)
enum StoerungsZustand {
    FEHLERFREI = 0,
    WARNUNG = 1,
    ALARM = 2
};

// Task-Variablen für den Loop
unsigned long lastVentilTime = 0;
const unsigned long ventilIntervall = 1000; // 1 Sekunde
unsigned long lastSensorTime = 0;
const unsigned long sensorIntervall = 2000; // 2 Sekunden
unsigned long lastNetzwerkTime = 0;
const unsigned long netzwerkIntervall = 5000; // 5 Sekunden

int anzahlPlaetze = 0;
int platzIndex = 0;

// --- ZEIT FUNKTIONEN ---
unsigned long getMillisekundenTimestamp() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    unsigned long mss = (unsigned long)(tv.tv_sec) * 1000 + (tv.tv_usec / 1000);
    return mss;
}

String getFormatiertenZeitstempel() {
    struct timeval tv;
    gettimeofday(&tv, NULL);

    time_t nowSecs = tv.tv_sec;
    struct tm timeinfo;
    localtime_r(&nowSecs, &timeinfo);

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

// Zeit vom lokalen Raspberry Pi holen
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

    void leseSensoren() {
        Serial.printf("[%s] Lese Sensoren fuer Platz %s (ID: %d)\n",
                      getFormatiertenZeitstempel().c_str(),
                      platzName.c_str(),
                      platzId);
    }

    void sendeSensorenDaten() { /* Logik kommt später */ }
};

std::vector<Platz> gartenPlaetze;

// --- NETZWERK FUNKTIONEN ---

bool meldeNeustart() {
    Serial.println("Versuche Wiederanlauf an Backend zu melden...");

    WiFiClientSecure secureClient;
    secureClient.setInsecure();
    secureClient.setTimeout(15000);

    HTTPClient http;
    // Beispiel: Falls du lokal über HTTP statt HTTPS testest, hier anpassen (ansonsten secureClient nutzen)
    if (http.begin(serverBaseUrl + "/api/arduino/reboot")) {
        http.addHeader("Content-Type", "application/json");

        String payload = "{\"status\": \"reboot\", \"grund\": \"power_on\"}";
        int httpCode = http.POST(payload);

        bool erfolg = (httpCode == HTTP_CODE_OK || httpCode == 200 || httpCode == 201);
        if (erfolg) {
            Serial.println("Neustart erfolgreich ans Backend gemeldet!");
        } else {
            Serial.printf("Backend nicht erreichbar. HTTP Code: %d\n", httpCode);
        }

        http.end();
        return erfolg;
    } else {
        Serial.println("HTTP Begin fehlgeschlagen!");
        return false;
    }
}

void sendeConfigAck() {
    HTTPClient http;
    if (http.begin(serverUrl[CONFIGURE_ACK])) {
        http.addHeader("Content-Type", "application/json");

        String payload = "{\"status\":\"ok\",\"plaetzeKonfiguriert\":" + String(gartenPlaetze.size()) + "}";
        int httpCode = http.POST(payload);

        if(httpCode > 0) {
            Serial.printf("Config-ACK gesendet. HTTP Code: %d\n", httpCode);
        } else {
            Serial.printf("Fehler beim Senden des Config-ACK: %d\n", httpCode);
        }
        http.end();
    }
}

bool ladeKonfigurationVomServer() {
    Serial.println("Versuche Konfiguration vom Server zu holen...");
    HTTPClient http;
    if (!http.begin(serverUrl[CONFIGURE])) {
        Serial.println("HTTP Begin für Config fehlgeschlagen!");
        return false;
    }

    int httpCode = http.GET();
    Serial.printf("Config HTTP Code: %d\n", httpCode);

    bool erfolg = false;
    if (httpCode == HTTP_CODE_OK || httpCode == 200) {
        String payload = http.getString();
        Serial.println("Payload empfangen:");
        Serial.println(payload);

        JsonDocument doc;
        DeserializationError error = deserializeJson(doc, payload);

        if (!error) {
            Serial.println("JSON erfolgreich geparst.");
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

    // Zeit-Synchronisation (Pi -> Fallback NTP)
    Serial.println("Versuche Zeit vom lokalen Raspberry Pi zu holen...");
    if (holeZeitVomRaspberry()) {
        Serial.println("Uhrzeit via Raspberry Pi synchronisiert: " + getFormatiertenZeitstempel());
    } else {
        Serial.println("Raspberry Pi nicht erreichbar. Versuche Internet-NTP...");
        configTime(gmtOffset_sec, daylightOffset_sec, ntpServer1, ntpServer2);

        struct tm timeinfo;
        if (getLocalTime(&timeinfo, 3000)) {
            Serial.println("Uhrzeit via Internet synchronisiert: " + getFormatiertenZeitstempel());
        } else {
            Serial.println("WARNUNG: Weder Internet noch Raspberry Pi erreichbar!");
        }
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

    // Task 3: Netzwerk (z.B. regelmäßige Befehlsabfrage)
    if (currentMillis - lastNetzwerkTime >= netzwerkIntervall) {
        if (WiFi.status() == WL_CONNECTED) {
            // Hier kannst du bei Bedarf den checkCommandsFromPi() einbauen
        }
        lastNetzwerkTime = currentMillis;
    }
}