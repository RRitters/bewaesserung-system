package com.example.bewaesserung.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/arduino")
public class ArduinoConfigController {

    public record SlotDto(int typId, int modbusAddr) {}

    public record VentilDto(
            int modbusId,
            int laufzeitMs,
            double sollDurchflussLpm,
            double minDurchflussLpm
    ) {}

    public record PlatzConfigResponse(
            long platzId,
            String platzName,
            VentilDto ventil,
            List<SlotDto> slots
    ) {}

    @GetMapping("/configure")
    public ResponseEntity<List<PlatzConfigResponse>> getConfiguration() {
        List<PlatzConfigResponse> configList = new ArrayList<>();

        // Beispiel für Platz 12: Sensoren (1..19) + Ventil (>= 20)
        List<SlotDto> slotsPlatz12 = List.of(
                new SlotDto(1, 1), // TYP_ID 1: 2-in-1 (10 cm) an Modbus 1
                new SlotDto(2, 2), // TYP_ID 2: 7-in-1 (35 cm) an Modbus 2
                new SlotDto(3, 3), // TYP_ID 3: Luft an Modbus 3
                new SlotDto(4, 4)  // TYP_ID 4: Durchfluss an Modbus 4
        );
        VentilDto ventilPlatz12 = new VentilDto(20, 10000, 0.0, 6.0);
        configList.add(new PlatzConfigResponse(12L, "Durian_Platz_12", ventilPlatz12, slotsPlatz12));

        // Beispiel für Platz 13
        List<SlotDto> slotsPlatz13 = List.of(
                new SlotDto(1, 1),
                new SlotDto(2, 2),
                new SlotDto(3, 3),
                new SlotDto(4, 4)
        );
        VentilDto ventilPlatz13 = new VentilDto(20, 10000, 0.0, 6.0);
        configList.add(new PlatzConfigResponse(13L, "Durian_Platz_13", ventilPlatz13, slotsPlatz13));

        return ResponseEntity.ok(configList);
    }
}