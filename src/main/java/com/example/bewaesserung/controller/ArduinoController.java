package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.Messwert;
import com.example.bewaesserung.model.Platz;
import com.example.bewaesserung.repository.MesswertRepository;
import com.example.bewaesserung.repository.PlatzRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arduino")
public class ArduinoController {

    @Autowired
    private PlatzRepository platzRepository;

    // TODO: Dein bestehendes Repository/Service zum Speichern in MESSWERTE einfügen, z. B.:
    @Autowired
    private MesswertRepository messwertRepository;

    public record SensorWertItem(
            Integer typId,
            Integer modbusAddr,
            Double feuchte,
            Double temperatur,
            Double ec,
            Double ph,
            Double n,
            Double p,
            Double k,
            Double durchflussLpm
    ) {}

    public record ArduinoMessdatenRequest(
            Long platzId,
            String zeitstempel,
            Boolean simulation,
            List<SensorWertItem> werte
    ) {}

    public record ArduinoDataResponse(
            String status,
            Long platzId,
            double sollDurchflussLpm
    ) {}

    @PostMapping("/reboot")
    public ResponseEntity receiveReboot(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @PostMapping("/data")
    public ResponseEntity<ArduinoDataResponse> receiveData(@RequestBody ArduinoMessdatenRequest req) {
        if (req == null || req.platzId() == null || req.werte() == null || req.werte().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Messwert messwert = new Messwert();
        messwert.setPlatzId(req.platzId());
        messwert.setGemessenAm(LocalDateTime.now().withNano(0));

        for (SensorWertItem w : req.werte()) {
            if (w.typId() == null) continue;

            switch (w.typId()) {
                case 1 -> { // 2-in-1 Boden (10 cm)
                    if (w.feuchte() != null) messwert.setFeuchte10cm(w.feuchte());
                    if (w.temperatur() != null) messwert.setTemp10cm(w.temperatur());
                }
                case 2 -> { // 7-in-1 Boden (35 cm)
                    if (w.feuchte() != null) messwert.setFeuchte35cm(w.feuchte());
                    if (w.temperatur() != null) messwert.setTemp35cm(w.temperatur());
                    if (w.ec() != null) messwert.setEcWert(w.ec());
                    if (w.ph() != null) messwert.setPhWert(w.ph());
                    if (w.n() != null) messwert.setStickstoff(w.n());
                    if (w.p() != null) messwert.setPhosphor(w.p());
                    if (w.k() != null) messwert.setKalium(w.k());
                }
                case 3 -> { // Luft
                    if (w.temperatur() != null) messwert.setLuftTemperatur(w.temperatur());
                    if (w.feuchte() != null) messwert.setLuftFeuchte(w.feuchte());
                }
                case 4 -> { // Durchfluss
                    if (w.durchflussLpm() != null) messwert.setDurchfluss(w.durchflussLpm());
                }
            }
        }

        // Hier deinen bisherigen Repository-/Service-Aufruf zum Speichern beibehalten, z. B.:
        messwertRepository.save(messwert);

        double sollLpm = platzRepository.findById(req.platzId())
                .map(Platz::getSollDurchfluss)
                .orElse(Platz.SOLL_DURCHFLUSS_MAX);

        return ResponseEntity.ok(new ArduinoDataResponse("OK", req.platzId(), sollLpm));
    }

    @PostMapping("/event")
    public ResponseEntity receiveEvent(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}