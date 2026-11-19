package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.Messwert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// Muss in der Datei MesswertRepository.java stehen:
public interface MesswertRepository extends JpaRepository<Messwert, Long> {
    // Den allerletzten Messwert für einen bestimmten Platz holen (für die Steuerung)
    Optional<Messwert> findFirstByPlatzIdOrderByGemessenAmDesc(Long platzId);

    // Die letzten N Messwerte für einen Platz holen (z.B. für Trend / gleitenden Mittelwert)
    List<Messwert> findTop5ByPlatzIdOrderByGemessenAmDesc(Long platzId);
}