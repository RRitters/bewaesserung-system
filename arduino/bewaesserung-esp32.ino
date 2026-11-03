#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <time.h>

// --- WLAN DATEN ---
const char* ssid = "Nathamon2508"; 
const char* password = "0611546046";

// --- ZENTRALE SERVER-EINSTELLUNGEN ---
const char* SERVER_IP = "192.168.1.45";
const int   SERVER_PORT = 8080;

// Basis-URL für das lokale Netzwerk
String serverBaseUrl = "http://" + String(SERVER_IP) + ":" + String(SERVER_PORT);

// Alle Endpunkte zentral abgeleitet
String serverUrl[] = {
    serverBaseUrl + "/api/arduino/data",         // Index 0
    serverBaseUrl + "/api/arduino/order",        // Index 1
    serverBaseUrl + "/api/arduino/order/ack",    // Index 2
    serverBaseUrl + "/api/arduino/alert",        // Index 3
    serverBaseUrl + "/api/arduino/alert/ack",    // Index 4
    serverBaseUrl + "/api/arduino/configure",    // Index 5
    serverBaseUrl + "/api/arduino/configure/ack" // Index 6
};

// Endpunkt für die Zeitabfrage vom Raspberry Pi
String piUrl = serverBaseUrl + "/api/arduino/time";

// Zeit-Synchronisation: Exakt 1 Stunde (3.600.000 Millisekunden)
unsigned long lastTimeSync = 0;
const unsigned long timeSyncInterval = 3600000; 
unsigned long deviceRandomOffset = 0; // Individueller Offset in Millisekunden

void setup() {
  Serial.begin(115200);
  delay(1000);

  WiFi.disconnect(true);
  delay(1000);
  WiFi.mode(WIFI_STA);
  
  // WLAN verbinden
  Serial.print("Verbinde mit WLAN: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    attempts++;
    
    if (attempts > 30) {
      Serial.println("\n[FEHLER] Konnte keine Verbindung zum WLAN herstellen!");
      break; 
    }
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWLAN erfolgreich verbunden!");
    Serial.print("IP-Adresse des ESP32: ");
    Serial.println(WiFi.localIP());

    // --- GENIALER TRICK: Individuellen Offset aus MAC-Adresse berechnen ---
    // Wir nehmen die echten MAC-Bytes des ESP32
    uint8_t mac[6];
    WiFi.macAddress(mac);
    // Wir nehmen das letzte Byte (0 bis 255) und mappen es auf einen Bereich 
    // von 0 bis 59 Minuten (0 bis 3.540.000 Millisekunden), damit sich jeder seine eigene Minute aussucht.
    deviceRandomOffset = (mac[5] * 13833UL); 
    
    Serial.print("Individueller Sync-Offset berechnet: ");
    Serial.print(deviceRandomOffset / 1000);
    Serial.println(" Sekunden nach der vollen Stunde.");

    // Initiale Zeiteinstellung beim Start sofort holen
    syncTimeWithPi();
    
    // WICHTIG: Wir tun so, als ob der letzte Sync vor (1 Stunde - Offset) stattgefunden hätte,
    // damit er sich exakt zu seiner individuellen Zeit das erste Mal regulär meldet.
    lastTimeSync = millis() - timeSyncInterval + deviceRandomOffset;
  }
}

void loop() {
  // Regelmäßige Zeitsynchronisation (wird nur ausgeführt, wenn 1 Stunde um ist)
  if (millis() - lastTimeSync > timeSyncInterval) {
    syncTimeWithPi();
    lastTimeSync = millis();
  }

  // Hier kommt später deine normale Hauptschleife hin (z.B. Sensoren abfragen, Daten senden etc.)
  
  delay(1000); // Kleine Pause im Loop, schont die CPU
}

// Funktion, um die Uhrzeit vom Raspberry Pi / der VM zu holen
void syncTimeWithPi() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    Serial.println("Hole aktuelle Zeit von der VM (" + piUrl + ")...");
    
    http.begin(piUrl);
    int httpResponseCode = http.GET();

    if (httpResponseCode > 0) {
      String payload = http.getString();
      Serial.println("Antwort vom Server: " + payload);

      // JSON parsen
      JsonDocument doc;
      DeserializationError error = deserializeJson(doc, payload);

      if (!error) {
        unsigned long epochTime = doc["epochSecond"]; 
        
        // Systemzeit des ESP32 setzen
        struct timeval tv;
        tv.tv_sec = epochTime;
        tv.tv_usec = 0;
        settimeofday(&tv, NULL);
        
        Serial.println("Uhrzeit erfolgreich vom Pi synchronisiert!");
      } else {
        Serial.print("Fehler beim Parsen der Zeit-JSON: ");
        Serial.println(error.c_str());
      }
    } else {
      Serial.print("Fehler beim HTTP-Request (Zeit), Code: ");
      Serial.println(httpResponseCode);
    }
    http.end();
  } else {
    Serial.println("Keine WLAN-Verbindung beim Zeit-Sync!");
  }
}
