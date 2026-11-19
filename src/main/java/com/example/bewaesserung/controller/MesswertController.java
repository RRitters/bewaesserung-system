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
        return repository.findTop5ByPlatzIdOrderByGemessenAmDesc(sensorId);
    }

    @PostMapping
    public Messwert createMesswert(@RequestBody Messwert messwert) {
        if (messwert.getGemessenAm() == null) {
            messwert.setGemessenAm(LocalDateTime.now().withNano(0));
        }
        return repository.save(messwert);
    }

    // Angepasster Test-Endpunkt für das neue kompakte Modell
    @GetMapping("/generate")
    public Messwert generateDummyMeasurement() {
        Messwert m = new Messwert();
        m.setPlatzId(1L); // Platz-ID statt Sensor-ID
        m.setBodenFeuchte(42.5); // Bodenfeuchte statt setWert
        m.setBodenTemperatur(25.0);
        m.setLuftTemperatur(23.5);
        m.setLuftFeuchte(60.0);
        m.setDurchfluss(0.0);
        m.setGemessenAm(LocalDateTime.now().withNano(0));
        return repository.save(m);
    }
}