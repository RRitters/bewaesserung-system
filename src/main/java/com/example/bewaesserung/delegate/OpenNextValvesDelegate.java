package com.example.bewaesserung.delegate;

import com.example.bewaesserung.model.Platz;
import com.example.bewaesserung.model.Ventil;
import com.example.bewaesserung.repository.PlatzRepository;
import com.example.bewaesserung.service.HardwareSteuerungsService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 3. Schritt in der Schleife: "Open Valve on ready place Max Y"
 */
@Component("openNextValvesDelegate")
public class OpenNextValvesDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(OpenNextValvesDelegate.class);

    private final PlatzRepository platzRepository;
    private final HardwareSteuerungsService hardwareService;

    public OpenNextValvesDelegate(PlatzRepository platzRepository, HardwareSteuerungsService hardwareService) {
        this.platzRepository = platzRepository;
        this.hardwareService = hardwareService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Integer freieSlots = (Integer) execution.getVariable("freieSlots");
        int slots = freieSlots != null ? freieSlots : 0;

        if (slots > 0) {
            List<Platz> wartendePlaetze = platzRepository
                    .findByBereichAndQueueStatusOrderByPrioritaetAsc("DURIAN", "QUEUED");

            int geoeffnet = 0;
            for (Platz platz : wartendePlaetze) {
                if (geoeffnet >= slots) break;

                log.info("Starte Bewässerung für nächsten Platz in Queue: {} (Prio {})",
                        platz.getName(), platz.getPrioritaet());

                for (Ventil v : platz.getVentile()) {
                    hardwareService.oeffneVentil(v);
                }
                platz.setQueueStatus("WATERING");
                platzRepository.save(platz);
                geoeffnet++;
            }
        }

        // Prüfen, ob noch irgendein Platz in DURIAN auf QUEUED oder WATERING steht
        boolean nochArbeitDa = platzRepository.existsByBereichAndQueueStatusIn(
                "DURIAN", List.of("QUEUED", "WATERING"));

        execution.setVariable("allPlacesDone", !nochArbeitDa);
    }
}