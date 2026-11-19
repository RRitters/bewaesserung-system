package com.example.bewaesserung.mock;

import com.example.bewaesserung.model.Messwert;
import com.example.bewaesserung.model.Platz;
import com.example.bewaesserung.repository.MesswertRepository;
import com.example.bewaesserung.repository.PlatzRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component("getSensorDataDelegate")
public class GetSensorDataDelegate implements JavaDelegate {

    @Autowired
    private PlatzRepository platzRepository;

    @Autowired
    private MesswertRepository messwertRepository;

    private final Map<Long, Integer> offeneZyklenProPlatz = new HashMap<>();
    private final Random random = new Random();

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        List<Platz> allePlaetze = platzRepository.findAll();

        int anzahlWatering = 0;
        int anzahlQueued = 0;
        int anzahlFinished = 0;
        int anzahlIdle = 0;
        int anzahlTrocken = 0;
        int schliessenPrioritaetCount = 0;
        int gespeicherteMesswerte = 0;

        LocalDateTime jetzt = LocalDateTime.now().withNano(0); // Auf volle Sekunden gekürzt

        for (Platz platz : allePlaetze) {
            String status = platz.getQueueStatus() != null ? platz.getQueueStatus().toUpperCase() : "IDLE";
            boolean ventilOffen = "WATERING".equals(status);

            switch (status) {
                case "WATERING": anzahlWatering++; break;
                case "QUEUED":   anzahlQueued++;   break;
                case "FINISHED": anzahlFinished++; break;
                default:         anzahlIdle++;     break;
            }

            double f10 = platz.getFeuchte10Cm() != null ? platz.getFeuchte10Cm() : (21.0 + random.nextDouble() * 8.0);
            double f35 = platz.getFeuchte35Cm() != null ? platz.getFeuchte35Cm() : (19.0 + random.nextDouble() * 7.0);

            if (ventilOffen) {
                int zyklen = offeneZyklenProPlatz.getOrDefault(platz.getId(), 0) + 1;
                offeneZyklenProPlatz.put(platz.getId(), zyklen);

                // Totzeit-Simulation: 10cm reagiert nach einigen Minuten/Zyklen, 35cm verzögert danach
                if (zyklen > 3) {
                    f10 = Math.min(85.0, f10 + 1.8 + random.nextDouble() * 1.2);
                }
                if (zyklen > 6) {
                    f35 = Math.min(80.0, f35 + 0.9 + random.nextDouble() * 0.7);
                }

                double istLiter = platz.getIstLiter() != null ? platz.getIstLiter() : 0.0;
                platz.setIstLiter(Math.round((istLiter + 2.5) * 10.0) / 10.0);
            } else {
                offeneZyklenProPlatz.put(platz.getId(), 0);
                // Langsames Absinken über Tag & Nacht
                f10 = Math.max(15.0, f10 - (0.08 + random.nextDouble() * 0.05));
                f35 = Math.max(15.0, f35 - (0.04 + random.nextDouble() * 0.03));
            }

            f10 = Math.round(f10 * 10.0) / 10.0;
            f35 = Math.round(f35 * 10.0) / 10.0;

            platz.setFeuchte10Cm(f10);
            platz.setFeuchte35Cm(f35);
            platzRepository.save(platz);

            // Schließen hat Priorität: Sobald 10cm oder 35cm Soll-Max (50%) erreicht
            if (ventilOffen && (f10 >= 50.0 || f35 >= 50.0)) {
                schliessenPrioritaetCount++;
            }
            if (f10 < 30.0 || f35 < 30.0) {
                anzahlTrocken++;
            }

            // NEU: Genau EINE kompakte Messwert-Zeile pro Platz und Zyklus speichern
            Messwert messwert = new Messwert();
            messwert.setPlatzId(platz.getId());
            messwert.setGemessenAm(jetzt);

            // Bodendaten
            messwert.setBodenFeuchte(f10);
            messwert.setBodenTemperatur(runden(26.5 + (random.nextDouble() * 3.0) - (ventilOffen ? 1.5 : 0.0)));
            messwert.setEcWert(runden(850.0 + (random.nextDouble() * 250.0)));
            messwert.setPhWert(runden(6.2 + (random.nextDouble() * 0.6)));
            messwert.setStickstoff(runden(45.0 + (random.nextDouble() * 15.0)));
            messwert.setPhosphor(runden(20.0 + (random.nextDouble() * 10.0)));
            messwert.setKalium(runden(110.0 + (random.nextDouble() * 30.0)));

            // Kleinklima (z. B. unter Bananenstauden / Bambus)
            messwert.setLuftTemperatur(runden(28.0 + (random.nextDouble() * 4.0)));
            messwert.setLuftFeuchte(runden(65.0 + (random.nextDouble() * 15.0)));

            // Durchfluss
            messwert.setDurchfluss(ventilOffen ? runden(4.2 + random.nextDouble() * 0.6) : 0.0);

            messwertRepository.save(messwert);
            gespeicherteMesswerte++;
        }

        boolean bodenZuTrocken = anzahlTrocken > 0;
        execution.setVariable("bodenZuTrocken", bodenZuTrocken);
        execution.setVariable("schliessenPrioritaetCount", schliessenPrioritaetCount);

        System.out.println(String.format(
                ">>> [MOCK-SENSOR] Boxen Gesamt: %d | WATERING (Offen): %d | QUEUED: %d | FINISHED: %d | IDLE: %d | Schließen-Prio: %d | Neue Messwerte: %d",
                allePlaetze.size(), anzahlWatering, anzahlQueued, anzahlFinished, anzahlIdle, schliessenPrioritaetCount, gespeicherteMesswerte
        ));
    }

    private double runden(double wert) {
        return Math.round(wert * 10.0) / 10.0;
    }
}