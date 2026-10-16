package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Messwert;
import com.example.bewaesserung.repository.MesswertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/measurements")
public class MesswertController {

    @Autowired
    private MesswertRepository repository;

    @GetMapping
    public List<Messwert> getAllMesswerte() {
        return repository.findAll();
    }

    @GetMapping("/sensor/{sensorId}")
    public List<Messwert> getMesswerteBySensor(@PathVariable Long sensorId) {
        return repository.findBySensorIdOrderByGemessenAmDesc(sensorId);
    }

    @PostMapping
    public Messwert createMesswert(@RequestBody Messwert messwert) {
        if (messwert.getGemessenAm() == null) {
            messwert.setGemessenAm(LocalDateTime.now());
        }
        return repository.save(messwert);
    }

    // Dein übernommener und angepasster Test-Endpunkt
    @GetMapping("/generate")
    public Messwert generateDummyMeasurement() {
        Messwert m = new Messwert();
        m.setSensorId(1L); // Passt perfekt zu deinem Sensor mit ID 1
        m.setWert(42.5);
        m.setEinheit("%");
        m.setGemessenAm(LocalDateTime.now());
        return repository.save(m);
    }
}