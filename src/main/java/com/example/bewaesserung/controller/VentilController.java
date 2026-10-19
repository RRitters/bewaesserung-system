package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Ventil;
import com.example.bewaesserung.repository.VentilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ventile")
public class VentilController {

    @Autowired
    private VentilRepository ventilRepository;

    @GetMapping
    public List<Ventil> getAllVentile() {
        return ventilRepository.findAll();
    }
}