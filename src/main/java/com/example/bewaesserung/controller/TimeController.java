package com.example.bewaesserung.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/arduino")
public class TimeController {

    @GetMapping("/time")
    public ResponseEntity<Map<String, Object>> getLocalTime() {
        Map<String, Object> response = new HashMap<>();
        Instant now = Instant.now();

        // Wir übergeben die Epochen-Millisekunden, damit der ESP32
        // den System-Timer sekundengenau setzen kann.
        response.put("epochSecond", now.getEpochSecond());
        response.put("epochMillis", now.toEpochMilli());

        return ResponseEntity.ok(response);
    }
}