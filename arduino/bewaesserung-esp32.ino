#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <vector>
#include <ArduinoJson.h>
#include <time.h>
#include <sys/time.h> // Wichtig für gettimeofday

// --- DEINE ECHTEN WLAN DATEN ---
#define WIFI_SSID "Nathamon2508"
#define WIFI_PASSWORD "0611546046"

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

const char* ntpServer1 = "pool.ntp.org";
const char* ntpServer2 = "time.nist.gov";
const long  gmtOffset_sec = 25200;     // 7 Stunde für MEZ (bzw. 7200 für Sommerzeit / MESZ)
const int   daylightOffset_sec = 0; // Sommerzeit-Offset in Sekunden (wird addiert, wenn aktiv)

// --- 

// --- LOKALES ZEIT-FALLBACK VOM RASPBERRY PI ---
bool holeZeitVomRaspberry() {
    Serial.println("Versuche lokale Zeit vom Raspberry Pi zu holen...");
    
    WiFiClient client; // Ganz normaler Client fürs lokale Netz (kein Secure/HTTPS nötig)
    HTTPClient http;
    
    // IP-Adresse deines Raspberry Pi im lokalen Netzwerk anpassen!
    String piUrl = "http://192.168.1.100:8080/api/arduino/time";
    
    if (http.begin(client, piUrl)) {
        int httpCode = http.GET();
        if (httpCode == HTTP_CODE_OK) {
            String payload = http.getString();
            
            // JSON parsen
            JsonDocument doc;
            DeserializationError error = deserializeJson(doc, payload);
            if (!error) {
                unsigned long epochMillis = doc["epochMillis"];
                
                // Zeit manuell im ESP32 System-Timer setzen
                struct timeval tv;
                tv.tv_sec = epochMillis / 1000;
                tv.tv_usec = (epochMillis % 1000) * 1000;
                settimeofday(&tv, NULL);
                
                Serial.println("Erfolgreich lokale Zeit vom Raspberry Pi übernommen!");
                http.end();
                return true;
            }
        }
        http.end();
    }
    Serial.println("Fehler beim Abrufen der lokalen Raspberry-Zeit!");
    return false;
}

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
        // Test-Ausgabe mit Millisekunden-Zeitstempel:
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
    if (http.begin(secureClient, "throwback-announcer-upfront.ngrok-free.dev", 443, "/api/arduino/reboot", true)) {
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
        return erfolg;
    } else {
        Serial.println("HTTP Begin fehlgeschlagen!");
        return false;
    }
}

void sendeConfigAck() {
    WiFiClientSecure secureClient;
    secureClient.setInsecure();
    secureClient.setTimeout(15000);
    
    HTTPClient http;
    if (http.begin(secureClient, serverUrl[CONFIGURE_ACK])) {
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
    }
}

bool ladeKonfigurationVomServer() {
    Serial.println("Versuche Konfiguration vom Server zu holen...");
    WiFiClientSecure secureClient;
    secureClient.setInsecure();
    secureClient.setTimeout(15000);
    
    HTTPClient http;
    if (!http.begin(secureClient, serverUrl[CONFIGURE])) {
        Serial.println("HTTP Begin für Config fehlgeschlagen!");
        return false;
    }

    http.addHeader("ngrok-skip-browser-warning", "true");
    
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

    // --- SICHERE ZEIT-SYNCHRONISATION (OHNE CRASH) ---
    // Wir versuchen zuerst das lokale Fallback zum Raspberry Pi.
    // Das schützt uns sofort, wenn das Internet (wie in Thailand üblich) tot ist.
    Serial.println("Versuche Zeit vom lokalen Raspberry Pi zu holen...");
    
    if (holeZeitVomRaspberry()) {
        Serial.println("Uhrzeit via Raspberry Pi synchronisiert: " + getFormatiertenZeitstempel());
    } else {
        Serial.println("Raspberry Pi nicht erreichbar. Versuche Internet-NTP...");
        
        // Erst wenn der Pi nicht da ist, versuchen wir das Internet (mit konfiguriertem Offset)
        configTime(gmtOffset_sec, daylightOffset_sec, ntpServer1, ntpServer2);
        
        struct tm timeinfo;
        // Kurzer Versuch (max 3 Sekunden), um Hänger zu vermeiden
        if (getLocalTime(&timeinfo, 3000)) {
            Serial.println("Uhrzeit via Internet synchronisiert: " + getFormatiertenZeitstempel());
        } else {
            Serial.println("WARNUNG: Weder Internet noch Raspberry Pi erreichbar! Uhrzeit läuft ab Start-Null.");
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

    // Task 3: Netzwerk
    if (currentMillis - lastNetzwerkTime >= netzwerkIntervall) {
        if (WiFi.status() == WL_CONNECTED) {
            // ...
        }
        lastNetzwerkTime = currentMillis;
    }
}
