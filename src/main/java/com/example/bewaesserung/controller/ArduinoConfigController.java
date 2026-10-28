package com.example.bewaesserung.controller;

import com.example.bewaesserung.dto.PlatzConfigDto;
import com.example.bewaesserung.dto.ArduinoAckDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/arduino")
public class ArduinoConfigController {

    @GetMapping("/configure")
    public ResponseEntity<List<PlatzConfigDto>> getConfiguration() {
        List<PlatzConfigDto> configList = new ArrayList<>();

        // Konfiguration für Platz 12 (Aus deiner DB)
        PlatzConfigDto platz12 = new PlatzConfigDto();
        platz12.setPlatzId(12L);
        platz12.setPlatzName("Durian_Platz_12");
        // Sensoren: Durchfluss(3) + Boden 10cm(10,11,12) + Boden 35cm(20,21,22)
        platz12.setSensorModbusIds(Arrays.asList(3, 10, 11, 12, 20, 21, 22));
        // Ventil: Kugelhahn über Relais
        platz12.setVentilModbusId(4);
        platz12.setVentilLaufzeitMs(10000);
        platz12.setSollPosition(0);
        configList.add(platz12);

        // Konfiguration für Platz 13 (Aus deiner DB)
        PlatzConfigDto platz13 = new PlatzConfigDto();
        platz13.setPlatzId(13L);
        platz13.setPlatzName("Durian_Platz_13");
        platz13.setSensorModbusIds(Arrays.asList(3, 10, 11, 12, 20, 21, 22));
        platz13.setVentilModbusId(4);
        platz13.setVentilLaufzeitMs(10000);
        platz13.setSollPosition(0);
        configList.add(platz13);

        return ResponseEntity.ok(configList);
    }
}
