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
}