package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Messwert;
import com.example.bewaesserung.model.SystemEventLog;
import com.example.bewaesserung.repository.MesswertRepository;
import com.example.bewaesserung.repository.PlatzRepository;
import com.example.bewaesserung.repository.SystemEventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/arduino")
public class ArduinoController {

    @Autowired
    private MesswertRepository messwertRepository;

    @Autowired
    private SystemEventLogRepository eventLogRepository;

    @Autowired
    private PlatzRepository platzRepository;

    public record SensorWertDto(
            Integer typId,
            Integer modbusAddr,
            Double feuchte,
            Double temperatur,
            Double ec,
            Double ph,
            Double n,
            Double p,
            Double k,
            Double durchflussLpm
    ) {}

    public record ArduinoBatchDataDto(
            Long platzId,
            String zeitstempel,
            Boolean simulation,
            List<SensorWertDto> werte
    ) {}

    @PostMapping("/data")
    public ResponseEntity<String> receiveSensorData(@RequestBody ArduinoBatchDataDto data) {
        if (data == null || data.platzId() == null || data.werte() == null || data.werte().isEmpty()) {
            return ResponseEntity.badRequest().body("Ungueltige oder leere Sensordaten");
        }

        Messwert messwert = new Messwert();
        messwert.setPlatzId(data.platzId());
        messwert.setGemessenAm(LocalDateTime.now().withNano(0));

        for (SensorWertDto w : data.werte()) {
            if (w.typId() == null) continue;

            switch (w.typId()) {
                case 1 -> { // 2-in-1 Boden (10 cm)
                    if (w.feuchte() != null) messwert.setFeuchte10cm(w.feuchte());
                    if (w.temperatur() != null) messwert.setTemp10cm(w.temperatur());
                }
                case 2 -> { // 7-in-1 Boden (35 cm)
                    if (w.feuchte() != null) messwert.setFeuchte35cm(w.feuchte());
                    if (w.temperatur() != null) messwert.setTemp35cm(w.temperatur());
                    if (w.ec() != null) messwert.setEcWert(w.ec());
                    if (w.ph() != null) messwert.setPhWert(w.ph());
                    if (w.n() != null) messwert.setStickstoff(w.n());
                    if (w.p() != null) messwert.setPhosphor(w.p());
                    if (w.k() != null) messwert.setKalium(w.k());
                }
                case 3 -> { // Luft
                    if (w.temperatur() != null) messwert.setLuftTemperatur(w.temperatur());
                    if (w.feuchte() != null) messwert.setLuftFeuchte(w.feuchte());
                }
                case 4 -> { // Durchfluss
                    if (w.durchflussLpm() != null) messwert.setDurchfluss(w.durchflussLpm());
                }
            }
        }

        messwertRepository.save(messwert);

        // Live-Status im Platz aktualisieren, damit Dashboard & Camunda die echten ESP32-Werte sehen
        platzRepository.findById(data.platzId()).ifPresent(platz -> {
            if (messwert.getFeuchte10cm() != null) {
                platz.setFeuchte10Cm(messwert.getFeuchte10cm());
            }
            if (messwert.getFeuchte35cm() != null) {
                platz.setFeuchte35Cm(messwert.getFeuchte35cm());
            }
            platzRepository.save(platz);
        });

        System.out.printf("Messwert gespeichert fuer Platz %d (Simulation=%s) | 10cm=%.1f%% (%.1f°C), 35cm=%.1f%% (%.1f°C), Luft=%.1f°C, Flow=%.2f l/min%n",
                data.platzId(), data.simulation(),
                messwert.getFeuchte10cm(), messwert.getTemp10cm(),
                messwert.getFeuchte35cm(), messwert.getTemp35cm(),
                messwert.getLuftTemperatur(), messwert.getDurchfluss());

        return ResponseEntity.ok("Messwert gespeichert");
    }

    @PostMapping("/reboot")
    public ResponseEntity<String> handleReboot(@RequestBody RebootRequest request) {
        System.out.println("Arduino Neustart gemeldet! Status: " + request.status() + ", Grund: " + request.grund());
        logEvent("INFO", "ESP32 Neustart gemeldet. Grund: " + request.grund());
        return ResponseEntity.ok("{\"status\":\"ok\"}");
    }

    @PostMapping("/configure/ack")
    public ResponseEntity<String> handleConfigAck(@RequestBody String payload) {
        System.out.println("Config-ACK empfangen: " + payload);
        return ResponseEntity.ok("ACK erhalten");
    }

    private void logEvent(String typ, String nachricht) {
        SystemEventLog event = new SystemEventLog(typ, "SENSORIK", null, nachricht);
        eventLogRepository.save(event);
    }
}

record RebootRequest(String status, String grund) {}