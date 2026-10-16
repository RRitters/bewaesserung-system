package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Aktor;
import com.example.bewaesserung.repository.AktorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actuators")
public class AktorController {

    @Autowired
    private AktorRepository repository;

    @GetMapping
    public List<Aktor> getAllAktoren() {
        return repository.findAll();
    }

    @PostMapping
    public Aktor createAktor(@RequestBody Aktor aktor) {
        return repository.save(aktor);
    }
}