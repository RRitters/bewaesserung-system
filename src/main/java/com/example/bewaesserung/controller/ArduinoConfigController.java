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

        // Platz 21: Reihe 1 - Platz 1 (existiert in Oracle PLATZ)
        List<SlotDto> slotsPlatz21 = List.of(
                new SlotDto(1, 1), // TYP_ID 1: 2-in-1 (10 cm) an Modbus 1
                new SlotDto(2, 2), // TYP_ID 2: 7-in-1 (35 cm) an Modbus 2
                new SlotDto(3, 3), // TYP_ID 3: Luft an Modbus 3
                new SlotDto(4, 4)  // TYP_ID 4: Durchfluss an Modbus 4
        );
        VentilDto ventilPlatz21 = new VentilDto(20, 10000, 0.0, 6.0);
        configList.add(new PlatzConfigResponse(21L, "Reihe 1 - Platz 1", ventilPlatz21, slotsPlatz21));

        // Platz 22: Reihe 1 - Platz 2 (existiert in Oracle PLATZ)
        List<SlotDto> slotsPlatz22 = List.of(
                new SlotDto(1, 1),
                new SlotDto(2, 2),
                new SlotDto(3, 3),
                new SlotDto(4, 4)
        );
        VentilDto ventilPlatz22 = new VentilDto(21, 10000, 0.0, 6.0);
        configList.add(new PlatzConfigResponse(22L, "Reihe 1 - Platz 2", ventilPlatz22, slotsPlatz22));

        return ResponseEntity.ok(configList);
    }
}