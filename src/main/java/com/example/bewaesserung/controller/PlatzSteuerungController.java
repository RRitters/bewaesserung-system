package com.example.bewaesserung.controller;

import com.example.bewaesserung.entity.PlatzSteuerung;
import com.example.bewaesserung.service.PlatzSteuerungService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/platz-steuerung")
public class PlatzSteuerungController {

    private final PlatzSteuerungService service;

    public PlatzSteuerungController(PlatzSteuerungService service) {
        this.service = service;
    }

    public record SollwerteRequest(
            BigDecimal sollLiter,
            BigDecimal pulsSollLiter,
            BigDecimal einsickerpauseSollMin,
            BigDecimal sollDurchflussLpm,
            boolean starteNeu
    ) {}

    public record FlowMessungRequest(
            Long platzId,
            double durchflussLpm
    ) {}

    @GetMapping("/{platzId}")
    public PlatzSteuerung getStatus(@PathVariable Long platzId) {
        return service.holeSteuerung(platzId);
    }

    @PostMapping("/{platzId}/sollwerte")
    public PlatzSteuerung speichereSollwerte(@PathVariable Long platzId,
                                             @RequestBody SollwerteRequest req) {
        return service.aktualisiereSollwerte(
                platzId,
                req.sollLiter(),
                req.pulsSollLiter(),
                req.einsickerpauseSollMin(),
                req.sollDurchflussLpm(),
                req.starteNeu()
        );
    }

    // Liefert dem ESP32 nach /api/arduino/data den aktuellen Soll-Durchfluss zurück
    @PostMapping("/flow-messung")
    public Map<String, Object> meldeFlow(@RequestBody FlowMessungRequest req) {
        PlatzSteuerung st = service.verarbeiteDurchflussMessung(req.platzId(), req.durchflussLpm());
        return Map.of(
                "platzId", st.getPlatzId(),
                "sollDurchflussLpm", st.getSollDurchflussLpm(),
                "zyklusStatus", st.getZyklusStatus(),
                "istLiter", st.getIstLiter()
        );
    }
}
