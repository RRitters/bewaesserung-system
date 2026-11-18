package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.Platz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatzRepository extends JpaRepository<Platz, Long> {

    // Alle Plätze eines bestimmten Bereichs finden (z.B. "DURIAN", "GARDEN", "MIDDLE_AREA")
    List<Platz> findByBereich(String bereich);

    // Alle Plätze eines Bereichs mit bestimmtem Queue-Status, sortiert nach Priorität (1 zuerst)
    List<Platz> findByBereichAndQueueStatusOrderByPrioritaetAsc(String bereich, String queueStatus);

    // Prüfen, ob in einem Bereich überhaupt noch aktive/wartende Plätze existieren
    boolean existsByBereichAndQueueStatusIn(String bereich, List<String> statuses);

    // Alle Plätze über alle Bereiche hinweg, die aktuell in einer der angegebenen Phasen sind
    List<Platz> findByQueueStatusIn(List<String> statuses);
}