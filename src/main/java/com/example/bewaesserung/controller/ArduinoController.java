package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.ArduinoDataDto;
import com.example.bewaesserung.model.Messwert;
import com.example.bewaesserung.model.Sensor;
import com.example.bewaesserung.repository.MesswertRepository;
import com.example.bewaesserung.repository.SensorRepository;
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

    @PostMapping("/data")
    public ResponseEntity<String> receiveSensorData(@RequestBody ArduinoDataDto data) {
        // 1. Welcher Sensor funkt hier?
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
        messwert.setEinheit(sensor.getEinheit()); // Einheit direkt vom Sensor übernehmen
        messwert.setGemessenAm(LocalDateTime.now());

        messwertRepository.save(messwert);

        return ResponseEntity.ok("Messwert erfolgreich gespeichert für Sensor: " + sensor.getName());
    }
}