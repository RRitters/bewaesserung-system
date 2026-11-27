package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Messwert;
import com.example.bewaesserung.repository.MesswertRepository;
import com.example.bewaesserung.service.PlatzSteuerungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/measurements")
public class MesswertController {

    @Autowired
    private MesswertRepository repository;

    @Autowired
    private PlatzSteuerungService platzSteuerungService;

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
        Messwert gespeichert = repository.save(messwert);

        // Durchfluss in PLATZ_STEUERUNG aufintegrieren und Einsickerpause / Stopp prüfen
        if (gespeichert.getPlatzId() != null && gespeichert.getDurchfluss() != null) {
            platzSteuerungService.verarbeiteDurchflussMessung(
                    gespeichert.getPlatzId(),
                    gespeichert.getDurchfluss()
            );
        }

        return gespeichert;
    }

    // Angepasster Test-Endpunkt für das neue kompakte Modell
    @GetMapping("/generate")
    public Messwert generateDummyMeasurement() {
        Messwert m = new Messwert();
        m.setPlatzId(21L); // Echte Platz-ID aus PLATZ
        m.setFeuchte10cm(42.5);
        m.setTemp10cm(25.0);
        m.setFeuchte35cm(47.5);
        m.setTemp35cm(24.0);
        m.setLuftTemperatur(23.5);
        m.setLuftFeuchte(60.0);
        m.setDurchfluss(15.6);
        m.setGemessenAm(LocalDateTime.now().withNano(0));
        return createMesswert(m);
    }
}