package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.ArduinoDataDto;
import com.example.bewaesserung.model.ValveCommandResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/arduino")
public class ArduinoController {

    /**
     * Endpoint für die Arduinos:
     * - Nimmt Messwerte entgegen
     * - Speichert sie (später in die DB / MESSWERTE-Tabelle)
     * - Gibt den Steuerungsbefehl für das Ventil als Antwort zurück
     */
    @PostMapping("/data")
    public ResponseEntity<ValveCommandResponse> receiveArduinoData(@RequestBody ArduinoDataDto data) {
        // 1. Hier würden die Daten in deine MESSWERTE / BEWAESSERUNG_LOG geschrieben werden
        System.out.println("Daten empfangen von Arduino: " + data.getArduinoId()
                + " für Platz " + data.getPlotId()
                + " | Feuchte: " + data.getSoilMoisture()
                + "% | Tank: " + data.getTankLevel() + "L");

        // 2. Logik zur Entscheidung (Beispiel: Wenn Feuchte unter 30 fällt, Ventil auf!)
        boolean shouldOpen = false;
        String msg = "Alles im grünen Bereich.";

        if (data.getSoilMoisture() != null && data.getSoilMoisture() < 30.0) {
            shouldOpen = true;
            msg = "Boden zu trocken. Ventil öffnen!";
            System.out.println("[BEFEHL] " + msg + " (Platz " + data.getPlotId() + ")");
        }

        // 3. Antwort an den Arduino zurückschicken
        ValveCommandResponse response = new ValveCommandResponse(data.getPlotId(), shouldOpen, msg);

        return ResponseEntity.ok(response);
    }
}