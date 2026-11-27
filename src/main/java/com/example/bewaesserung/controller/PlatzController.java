package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Platz;
import com.example.bewaesserung.repository.PlatzRepository;
import com.example.bewaesserung.service.PlatzSteuerungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/plaetze")
public class PlatzController {

    @Autowired
    private PlatzRepository platzRepository;

    @Autowired
    private PlatzSteuerungService platzSteuerungService;

    public record SollDurchflussBatchRequest(
            Long platzId,
            String scope, // "PLATZ", "REIHE" oder "ALLE"
            Double sollDurchflussLpm
    ) {}

    public record SollDurchflussBatchResponse(
            String status,
            String scope,
            int aktualisiertePlaetze,
            double sollDurchflussLpm
    ) {}

    public record GeoUpdateRequest(
            Double latitude,
            Double longitude
    ) {}

    // Alle Plätze abrufen (für die Übersicht auf dem Handy oder den Arduino-Start)
    @GetMapping
    public List<Platz> getAllPlaetze() {
        return platzRepository.findAll();
    }

    // Einen neuen Platz anlegen oder aktualisieren (Eingabe über das Handy)
    @PostMapping
    public ResponseEntity<Platz> savePlatz(@RequestBody Platz platz) {
        Platz savedPlatz = platzRepository.save(platz);
        return ResponseEntity.ok(savedPlatz);
    }

    // Geo-Position eines Platzes speichern
    @PutMapping("/{platzId}/geo")
    public ResponseEntity<Platz> updateGeo(@PathVariable Long platzId,
                                           @RequestBody GeoUpdateRequest req) {
        if (req == null || req.latitude() == null || req.longitude() == null) {
            return ResponseEntity.badRequest().build();
        }
        return platzRepository.findById(platzId)
                .map(p -> {
                    p.setLatitude(req.latitude());
                    p.setLongitude(req.longitude());
                    return ResponseEntity.ok(platzRepository.save(p));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Soll-Durchfluss auf einen Platz, eine ganze Reihe oder alle Plätze anwenden
    @PutMapping("/soll-durchfluss/batch")
    public ResponseEntity<SollDurchflussBatchResponse> updateSollDurchflussBatch(
            @RequestBody SollDurchflussBatchRequest req) {

        if (req == null || req.sollDurchflussLpm() == null) {
            return ResponseEntity.badRequest().build();
        }

        double clamped = Math.max(0.0, Math.min(Platz.SOLL_DURCHFLUSS_MAX, req.sollDurchflussLpm()));
        String scope = (req.scope() != null) ? req.scope().toUpperCase() : "PLATZ";

        List<Platz> zielPlaetze;

        switch (scope) {
            case "ALLE" -> zielPlaetze = platzRepository.findAll();

            case "REIHE" -> {
                if (req.platzId() == null) return ResponseEntity.badRequest().build();
                Platz referenz = platzRepository.findById(req.platzId()).orElse(null);
                if (referenz == null) return ResponseEntity.notFound().build();

                String reihenPrefix = extrahiereReihe(referenz.getName());
                zielPlaetze = platzRepository.findAll().stream()
                        .filter(p -> extrahiereReihe(p.getName()).equalsIgnoreCase(reihenPrefix))
                        .toList();
            }

            default -> { // "PLATZ"
                if (req.platzId() == null) return ResponseEntity.badRequest().build();
                zielPlaetze = platzRepository.findById(req.platzId())
                        .map(List::of)
                        .orElse(List.of());
            }
        }

        for (Platz p : zielPlaetze) {
            p.setSollDurchfluss(clamped);
            try {
                platzSteuerungService.aktualisiereSollwerte(
                        p.getId(), null, null, null, BigDecimal.valueOf(clamped), false
                );
            } catch (IllegalArgumentException ignored) {
                // Platz ohne PLATZ_STEUERUNG-Zeile
            }
        }
        platzRepository.saveAll(zielPlaetze);

        return ResponseEntity.ok(new SollDurchflussBatchResponse(
                "OK", scope, zielPlaetze.size(), clamped
        ));
    }

    private String extrahiereReihe(String platzName) {
        if (platzName == null) return "";
        int idx = platzName.indexOf(" - ");
        return (idx > 0) ? platzName.substring(0, idx).trim() : platzName.trim();
    }
}