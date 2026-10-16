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

    // Hilfsmethode für das Event-Logging
    private void logEvent(String typ, String nachricht) {
        // Nutzt deinen Konstruktor: category, groupName, plotId, message
        SystemEventLog event = new SystemEventLog(typ, "SENSORIK", null, nachricht);
        eventLogRepository.save(event);
    }
}