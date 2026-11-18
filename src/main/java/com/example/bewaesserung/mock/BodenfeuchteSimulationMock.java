package com.example.bewaesserung.mock;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class BodenfeuchteSimulationMock {

    private final Random random = new Random();

    // Speichert für jeden Platz ("Reihe_Platz") die Feuchte in 10cm und 35cm Tiefe
    private final Map<String, Double> feuchte10cm = new HashMap<>();
    private final Map<String, Double> feuchte35cm = new HashMap<>();

    // Zähler für die Aktivierungen
    private int totalOeffnungen = 0;
    private int totalSchliessungen = 0;

    public BodenfeuchteSimulationMock() {
        // Initialisiere alle 11 Reihen und 7 Plätze mit leicht unterschiedlichen Basiswerten (20% - 30%)
        for (int r = 1; r <= 11; r++) {
            for (int p = 1; p <= 7; p++) {
                String key = r + "_" + p;
                feuchte10cm.put(key, 20.0 + random.nextDouble() * 10.0);
                feuchte35cm.put(key, 18.0 + random.nextDouble() * 8.0);
            }
        }
    }

    public synchronized Map<String, Object> simuliereSchritt(int reihe, int platz, boolean ventilOffen) {
        String key = reihe + "_" + platz;
        double wert10 = feuchte10cm.getOrDefault(key, 25.0);
        double wert35 = feuchte35cm.getOrDefault(key, 22.0);

        if (ventilOffen) {
            totalOeffnungen++;
            // 10cm reagiert schneller (nach wenigen Minuten / Zyklen)
            wert10 += 3.5 + (random.nextDouble() * 2.0);
            // 35cm hat eine spürbare Totzeit und steigt langsamer nach
            wert35 += 1.2 + (random.nextDouble() * 1.0);

            if (wert10 > 85.0) wert10 = 85.0;
            if (wert35 > 75.0) wert35 = 75.0;
        } else {
            totalSchliessungen++;
            // Austrocknung (Nacht / Verdunstung)
            wert10 -= 0.3;
            wert35 -= 0.1;
            if (wert10 < 15.0) wert10 = 15.0;
            if (wert35 < 15.0) wert35 = 15.0;
        }

        feuchte10cm.put(key, wert10);
        feuchte35cm.put(key, wert35);

        Map<String, Object> result = new HashMap<>();
        result.put("feuchte10", wert10);
        result.put("feuchte35", wert35);
        // Schließen hat Priorität: Sobald 10cm oder 35cm zu feucht sind, wird geschlossen (True = zu trocken, False = Feuchte erreicht)
        boolean bodenZuTrocken = (wert10 < 45.0 && wert35 < 45.0);
        result.put("bodenZuTrocken", bodenZuTrocken);

        // Zähler für die Ausgaben mitliefern
        result.put("totalOeffnungen", totalOeffnungen);
        result.put("totalSchliessungen", totalSchliessungen);

        return result;
    }
}