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

import java.util.ArrayList;
import java.util.List;

/**
 * 1. Schritt in der Schleife: "Durian Queue & Ventile steuern"
 */
@Component("durianQueueControlDelegate")
public class DurianQueueControlDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(DurianQueueControlDelegate.class);

    // Maximale Anzahl gleichzeitig geöffneter Plätze (hydraulisches Limit der 48V-Pumpe)
    private static final int MAX_GLEICHZEITIGE_PLAETZE = 3;

    private final PlatzRepository platzRepository;

    public DurianQueueControlDelegate(PlatzRepository platzRepository) {
        this.platzRepository = platzRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        List<Platz> aktivePlaetze = platzRepository
                .findByBereichAndQueueStatusOrderByPrioritaetAsc("DURIAN", "WATERING");

        List<Long> zuSchliessendePlatzIds = new ArrayList<>();
        int verbleibendAktiv = 0;

        for (Platz p : aktivePlaetze) {
            double soll = p.getSollLiter() != null ? p.getSollLiter() : 0.0;
            double ist = p.getIstLiter() != null ? p.getIstLiter() : 0.0;

            if (ist >= soll) {
                log.info("Platz {} hat Soll-Menge erreicht ({}/{} L) -> markiere zum Schließen.",
                        p.getName(), ist, soll);
                p.setQueueStatus("FINISHING");
                platzRepository.save(p);
                zuSchliessendePlatzIds.add(p.getId());
            } else {
                verbleibendAktiv++;
            }
        }

        // Berechnen, wie viele freie Slots für wartende Plätze (QUEUED) entstehen
        int freieSlots = Math.max(0, MAX_GLEICHZEITIGE_PLAETZE - verbleibendAktiv);

        execution.setVariable("zuSchliessendePlatzIds", zuSchliessendePlatzIds);
        execution.setVariable("freieSlots", freieSlots);
    }
}