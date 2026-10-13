package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.SensorMeasurement;
import com.example.bewaesserung.repository.MeasurementRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/measurements")
public class MeasurementController {

    private final MeasurementRepository repository;

    public MeasurementController(MeasurementRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<SensorMeasurement> getAllMeasurements() {
        return repository.findAll();
    }

    // 2. Test-Messwert manuell erzeugen per Browser-Aufruf (GET)
    @GetMapping("/generate")
    public SensorMeasurement generateDummyMeasurement() {
        SensorMeasurement m = new SensorMeasurement();
        m.setSensorId(1L); // Bezieht sich auf einen Sensor
        m.setWert(42.5);
        m.setEinheit("%");
        m.setGemessenAm(LocalDateTime.now());
        return repository.save(m);
    }
}