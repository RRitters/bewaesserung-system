package com.example.bewaesserung.mock;

import com.example.bewaesserung.model.Platz;
import com.example.bewaesserung.repository.PlatzRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component("getSensorDataDelegate")
public class GetSensorDataDelegate implements JavaDelegate {

    @Autowired
    private PlatzRepository platzRepository;

    @Override
    @Transactional(readOnly = true)
    public void execute(DelegateExecution execution) throws Exception {
        List<Platz> allePlaetze = platzRepository.findAll();

        int anzahlWatering = 0;
        int anzahlQueued = 0;
        int anzahlFinished = 0;
        int anzahlIdle = 0;
        int anzahlTrocken = 0;
        int schliessenPrioritaetCount = 0;

        for (Platz platz : allePlaetze) {
            String status = platz.getQueueStatus() != null ? platz.getQueueStatus().toUpperCase() : "IDLE";
            boolean ventilOffen = "WATERING".equals(status);

            switch (status) {
                case "WATERING": anzahlWatering++; break;
                case "QUEUED":   anzahlQueued++;   break;
                case "FINISHED": anzahlFinished++; break;
                default:         anzahlIdle++;     break;
            }

            Double f10 = platz.getFeuchte10Cm();
            Double f35 = platz.getFeuchte35Cm();

            // Schließen hat Priorität: Sobald 10cm oder 35cm Soll-Max (50%) erreicht
            if (ventilOffen && ((f10 != null && f10 >= 50.0) || (f35 != null && f35 >= 50.0))) {
                schliessenPrioritaetCount++;
            }
            if ((f10 != null && f10 < 30.0) || (f35 != null && f35 < 30.0)) {
                anzahlTrocken++;
            }
        }

        boolean bodenZuTrocken = anzahlTrocken > 0;
        execution.setVariable("bodenZuTrocken", bodenZuTrocken);
        execution.setVariable("schliessenPrioritaetCount", schliessenPrioritaetCount);

        System.out.printf(
                ">>> [ESP32-SENSOR-CHECK] Boxen Gesamt: %d | WATERING: %d | QUEUED: %d | FINISHED: %d | IDLE: %d | Schließen-Prio: %d%n",
                allePlaetze.size(), anzahlWatering, anzahlQueued, anzahlFinished, anzahlIdle, schliessenPrioritaetCount
        );
    }
}