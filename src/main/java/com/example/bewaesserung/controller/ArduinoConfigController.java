package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Platz;
import com.example.bewaesserung.repository.PlatzRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arduino")
public class ArduinoConfigController {

    @Autowired
    private PlatzRepository platzRepository;

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

    public record DurchflussUpdateRequest(double sollDurchflussLpm) {}

    @GetMapping("/configure")
    public ResponseEntity<List<PlatzConfigResponse>> getConfiguration() {
        List<PlatzConfigResponse> configList = new ArrayList<>();

        List<SlotDto> standardSlots = List.of(
                new SlotDto(1, 1), // TYP_ID 1: 2-in-1 (10 cm) an Modbus 1
                new SlotDto(2, 2), // TYP_ID 2: 7-in-1 (35 cm) an Modbus 2
                new SlotDto(3, 3), // TYP_ID 3: Luft an Modbus 3
                new SlotDto(4, 4)  // TYP_ID 4: Durchfluss an Modbus 4
        );

        // Platz 21: Gespeicherten Soll-Durchfluss aus Oracle PLATZ laden (Default MAX = 24.0)
        double soll21 = platzRepository.findById(21L)
                .map(Platz::getSollDurchfluss)
                .orElse(24.0);
        VentilDto ventilPlatz21 = new VentilDto(20, 10000, soll21, 6.0);
        configList.add(new PlatzConfigResponse(21L, "Reihe 1 - Platz 1", ventilPlatz21, standardSlots));

        // Platz 22: Gespeicherten Soll-Durchfluss aus Oracle PLATZ laden (Default MAX = 24.0)
        double soll22 = platzRepository.findById(22L)
                .map(Platz::getSollDurchfluss)
                .orElse(24.0);
        VentilDto ventilPlatz22 = new VentilDto(21, 10000, soll22, 6.0);
        configList.add(new PlatzConfigResponse(22L, "Reihe 1 - Platz 2", ventilPlatz22, standardSlots));

        return ResponseEntity.ok(configList);
    }

    // Endpunkt für das Einmessen vor Ort per Smartphone: speichert den neuen Wert dauerhaft in PLATZ
    @PutMapping("/platz/{platzId}/durchfluss")
    public ResponseEntity<?> updateSollDurchfluss(
            @PathVariable Long platzId,
            @RequestBody DurchflussUpdateRequest request) {

        double begrenzt = Math.max(0.0, Math.min(24.0, Math.round(request.sollDurchflussLpm() * 10.0) / 10.0));

        return platzRepository.findById(platzId)
                .map(platz -> {
                    platz.setSollDurchfluss(begrenzt);
                    platzRepository.save(platz);
                    System.out.printf(">>> [HANDY-EINMESSUNG] Platz %d (%s): Neuer Soll-Durchfluss = %.1f l/min dauerhaft gespeichert.%n",
                            platzId, platz.getName(), begrenzt);
                    return ResponseEntity.ok(Map.of(
                            "platzId", platzId,
                            "platzName", platz.getName(),
                            "sollDurchflussLpm", begrenzt
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}