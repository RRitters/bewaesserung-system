package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Platz;
import com.example.bewaesserung.repository.PlatzRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plaetze")
public class PlatzController {

    @Autowired
    private PlatzRepository platzRepository;

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