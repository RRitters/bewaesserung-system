package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {
    // Spring Data JPA übernimmt hier automatisch die Standard-Datenbankabfragen (CRUD)
}