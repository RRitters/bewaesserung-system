package com.example.bewaesserung.service;

import com.example.bewaesserung.entity.PlatzSteuerung;
import com.example.bewaesserung.repository.PlatzSteuerungRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlatzSteuerungService {

    private final PlatzSteuerungRepository repository;

    // Merkt sich pro Platz den Zeitpunkt der letzten Durchfluss-Messung sowie den Ziel-Durchfluss während der Gießphasen
    private final Map<Long, Instant> letzteMessungZeit = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> giessSollDurchflussLpm = new ConcurrentHashMap<>();

    public PlatzSteuerungService(PlatzSteuerungRepository repository) {
        this.repository = repository;
    }

    public PlatzSteuerung holeSteuerung(Long platzId) {
        return repository.findById(platzId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannte PLATZ_ID: " + platzId));
    }

    @Transactional
    public PlatzSteuerung aktualisiereSollwerte(Long platzId,
                                                BigDecimal sollLiter,
                                                BigDecimal pulsSollLiter,
                                                BigDecimal einsickerpauseSollMin,
                                                BigDecimal sollDurchflussLpm,
                                                boolean starteNeu) {
        PlatzSteuerung st = holeSteuerung(platzId);

        if (sollLiter != null) {
            st.setSollLiter(sollLiter.max(BigDecimal.ZERO));
        }
        if (pulsSollLiter != null) {
            st.setPulsSollLiter(pulsSollLiter.max(BigDecimal.ZERO));
        }
        if (einsickerpauseSollMin != null) {
            st.setEinsickerpauseSollMin(einsickerpauseSollMin.max(BigDecimal.ZERO));
        }
        if (sollDurchflussLpm != null && sollDurchflussLpm.compareTo(BigDecimal.ZERO) > 0) {
            giessSollDurchflussLpm.put(platzId, sollDurchflussLpm);
            if (!"EINSICKERN".equals(st.getZyklusStatus())) {
                st.setSollDurchflussLpm(sollDurchflussLpm);
            }
        }

        if (starteNeu) {
            st.setIstLiter(BigDecimal.ZERO);
            st.setEinsickerpauseIstMin(BigDecimal.ZERO);
            st.setAnzahlBewaesserung(1);
            st.setAnzahlPause(0);

            BigDecimal ersterStopp = (st.getPulsSollLiter().compareTo(BigDecimal.ZERO) > 0
                    && st.getPulsSollLiter().compareTo(st.getSollLiter()) < 0)
                    ? st.getPulsSollLiter()
                    : st.getSollLiter();

            st.setNaechsteAbschaltungLiter(ersterStopp);

            if (st.getSollLiter().compareTo(BigDecimal.ZERO) > 0) {
                st.setZyklusStatus("GIESSEN");
                BigDecimal lpm = giessSollDurchflussLpm.getOrDefault(platzId,
                        st.getSollDurchflussLpm().compareTo(BigDecimal.ZERO) > 0
                                ? st.getSollDurchflussLpm()
                                : BigDecimal.valueOf(16));
                st.setSollDurchflussLpm(lpm);
                letzteMessungZeit.put(platzId, Instant.now());
            } else {
                st.setZyklusStatus("BEREIT");
                st.setSollDurchflussLpm(BigDecimal.ZERO);
            }
        }

        return repository.save(st);
    }

    @Transactional
    public PlatzSteuerung verarbeiteDurchflussMessung(Long platzId, double durchflussLpm) {
        PlatzSteuerung st = holeSteuerung(platzId);
        Instant jetzt = Instant.now();
        Instant vorher = letzteMessungZeit.put(platzId, jetzt);

        if (vorher != null && "GIESSEN".equals(st.getZyklusStatus()) && durchflussLpm > 0.0) {
            long sekunden = Math.max(1L, Duration.between(vorher, jetzt).getSeconds());
            double gelieferteLiter = durchflussLpm * (sekunden / 60.0);

            BigDecimal neueIstLiter = st.getIstLiter()
                    .add(BigDecimal.valueOf(gelieferteLiter))
                    .setScale(2, RoundingMode.HALF_UP);
            st.setIstLiter(neueIstLiter);

            // Gesamtziel erreicht?
            if (st.getSollLiter().compareTo(BigDecimal.ZERO) > 0
                    && neueIstLiter.compareTo(st.getSollLiter()) >= 0) {
                st.setIstLiter(st.getSollLiter());
                st.setZyklusStatus("FERTIG");
                st.setSollDurchflussLpm(BigDecimal.ZERO);
                return repository.save(st);
            }

            // Nächste Einsicker-Abschaltung erreicht?
            if (st.getNaechsteAbschaltungLiter().compareTo(BigDecimal.ZERO) > 0
                    && neueIstLiter.compareTo(st.getNaechsteAbschaltungLiter()) >= 0) {
                st.setZyklusStatus("EINSICKERN");
                st.setEinsickerpauseIstMin(BigDecimal.ZERO);
                st.setAnzahlPause(st.getAnzahlPause() + 1);
                st.setSollDurchflussLpm(BigDecimal.ZERO); // Ventil öffnet / kein Soll-Durchfluss während Pause

                BigDecimal naechsterStopp = st.getNaechsteAbschaltungLiter()
                        .add(st.getPulsSollLiter())
                        .min(st.getSollLiter());
                st.setNaechsteAbschaltungLiter(naechsterStopp);
            }
        }

        return repository.save(st);
    }

    // Läuft jede Minute (60.000 ms), um laufende Einsickerpausen fortzuschreiben
    @Scheduled(fixedRate = 60000L)
    @Transactional
    public void aktualisiereEinsickerpausen() {
        List<PlatzSteuerung> pausierende = repository.findByZyklusStatus("EINSICKERN");
        for (PlatzSteuerung st : pausierende) {
            BigDecimal neuePauseIst = st.getEinsickerpauseIstMin()
                    .add(BigDecimal.ONE)
                    .setScale(2, RoundingMode.HALF_UP);
            st.setEinsickerpauseIstMin(neuePauseIst);

            if (neuePauseIst.compareTo(st.getEinsickerpauseSollMin()) >= 0) {
                st.setZyklusStatus("GIESSEN");
                st.setAnzahlBewaesserung(st.getAnzahlBewaesserung() + 1);
                BigDecimal lpm = giessSollDurchflussLpm.getOrDefault(st.getPlatzId(), BigDecimal.valueOf(16));
                st.setSollDurchflussLpm(lpm);
                letzteMessungZeit.put(st.getPlatzId(), Instant.now());
            }
            repository.save(st);
        }
    }
}
