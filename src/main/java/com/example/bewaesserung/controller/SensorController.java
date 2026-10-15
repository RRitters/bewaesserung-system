package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Sensor;
import com.example.bewaesserung.repository.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    @Autowired
    private SensorRepository repository;

    // 1. Alle Sensoren abrufen (GET)
    @GetMapping
    public List<Sensor> getAllSensors() {
        return repository.findAll();
    }

    // 2. Einen neuen Sensor per API speichern (POST)
    @PostMapping
    public Sensor createSensor(@RequestBody Sensor sensor) {
        return repository.save(sensor);
    }
}