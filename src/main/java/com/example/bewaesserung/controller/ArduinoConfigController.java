package com.example.bewaesserung.controller;

import com.example.bewaesserung.dto.PlatzConfigDto;
import com.example.bewaesserung.dto.ArduinoAckDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/arduino")
public class ArduinoConfigController {

    // GET: http://192.168.1.45:8080/api/arduino/configure
    @GetMapping("/configure")
    public ResponseEntity<List<PlatzConfigDto>> getConfiguration() {

        // TODO: Später holen wir das über einen Service direkt aus der DB.
        // Für den ersten Test bauen wir uns hier Mock-Daten, die dem Arduino schmecken.

        List<PlatzConfigDto> configList = new ArrayList<>();

        PlatzConfigDto durian0 = new PlatzConfigDto();
        durian0.setPlatzId(101L); // Echte DB ID
        durian0.setPlatzName("Durian_0");
        durian0.setVentilPin(5);
        durian0.setModbusIds(Arrays.asList(10, 11, 12));
        durian0.setVentilKorrekturFaktor(1.0);
        durian0.setLetzteVentilStellung(0);
        configList.add(durian0);

        PlatzConfigDto durian1 = new PlatzConfigDto();
        durian1.setPlatzId(102L); // Echte DB ID
        durian1.setPlatzName("Durian_1");
        durian1.setVentilPin(6);
        durian1.setModbusIds(Arrays.asList(14, 15));
        durian1.setVentilKorrekturFaktor(1.05);
        durian1.setLetzteVentilStellung(45);
        configList.add(durian1);

        // Spring Boot wandelt diese Liste automatisch in ein sauberes JSON-Array um
        return ResponseEntity.ok(configList);
    }

    // POST: http://192.168.1.45:8080/api/arduino/configure/ack
    @PostMapping("/configure/ack")
    public ResponseEntity<String> acknowledgeConfiguration(@RequestBody ArduinoAckDto ackDaten) {

        System.out.println("ACK vom Arduino erhalten!");
        System.out.println("Status: " + ackDaten.getStatus());
        System.out.println("Konfigurierte Plätze: " + ackDaten.getPlaetzeKonfiguriert());

        // Hier könnte man später in der DB speichern: "Arduino ist online und synchron"

        return ResponseEntity.ok("{\"message\":\"ACK erfolgreich verarbeitet\"}");
    }
}