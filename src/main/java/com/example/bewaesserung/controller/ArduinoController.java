package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.ArduinoDataDto;
import com.example.bewaesserung.model.Messwert;
import com.example.bewaesserung.model.Sensor;
import com.example.bewaesserung.model.SystemEventLog;
import com.example.bewaesserung.repository.MesswertRepository;
import com.example.bewaesserung.repository.SensorRepository;
import com.example.bewaesserung.repository.SystemEventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/arduino")
public class ArduinoController {

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private MesswertRepository messwertRepository;

    @Autowired
    private SystemEventLogRepository eventLogRepository;

    @PostMapping("/data")
    public ResponseEntity<String> receiveSensorData(@RequestBody ArduinoDataDto data) {
        // 1. Sensor suchen
        Optional<Sensor> sensorOpt = sensorRepository.findByNodeIpAndModbusId(data.getNodeIp(), data.getModbusId());

        if (sensorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sensor nicht gefunden für Node: " + data.getNodeIp() + " und Modbus-ID: " + data.getModbusId());
        }

        Sensor sensor = sensorOpt.get();

        // 2. Messwert erstellen und speichern
        Messwert messwert = new Messwert();
        messwert.setSensorId(sensor.getId());
        messwert.setWert(data.getWert());
        messwert.setEinheit(sensor.getEinheit());
        messwert.setGemessenAm(LocalDateTime.now());
        messwertRepository.save(messwert);

        // 3. Schwellwert-Logik (Das Gehirn)
        String antwort = "Messwert gespeichert. ";

        if (sensor.getSollMin() != null && data.getWert() < sensor.getSollMin()) {
            antwort += "ACHTUNG: Zu trocken! Bewässerung erforderlich.";
            logEvent("WARNUNG", "Zu trocken: " + data.getWert() + sensor.getEinheit() + " an " + sensor.getName());

            // TODO: Hier später das Ventil (Aktor) ansteuern!

        } else if (sensor.getSollMax() != null && data.getWert() > sensor.getSollMax()) {
            antwort += "ACHTUNG: Zu feucht! Bewässerung stoppen.";
            logEvent("INFO", "Ausreichend feucht: " + data.getWert() + sensor.getEinheit() + " an " + sensor.getName());
        } else {
            antwort += "Alles im grünen Bereich.";
        }

        return ResponseEntity.ok(antwort);
    }

    // Neuer Endpunkt für die Neustart-Meldung des ESP32
    @PostMapping("/reboot")
    public ResponseEntity<String> handleReboot(@RequestBody RebootRequest request) {
        // Mugabe in der Linux-Konsole zur Kontrolle
        System.out.println("Arduino Neustart gemeldet! Status: " + request.status() + ", Grund: " + request.grund());

        // Optional: Event direkt in die Datenbank loggen
        logEvent("INFO", "ESP32 Neustart gemeldet. Grund: " + request.grund());

        // Wichtig für den ESP32: Er wartet auf ein erfolgreiches HTTP 200 (OK)
        return ResponseEntity.ok("{\"status\":\"ok\"}");
    }

    @PostMapping("/configure/ack")
    public ResponseEntity<String> handleConfigAck(@RequestBody String payload) {
        // Hier kannst du das JSON verarbeiten oder einfach nur loggen
        System.out.println("Config-ACK empfangen: " + payload);
        return ResponseEntity.ok("ACK erhalten");
    }

    // Hilfsmethode für das Event-Logging
    private void logEvent(String typ, String nachricht) {
        SystemEventLog event = new SystemEventLog(typ, "SENSORIK", null, nachricht);
        eventLogRepository.save(event);
    }
}

// Datenobjekt (Record) für den JSON-Empfang des Neustarts
record RebootRequest(String status, String grund) {}