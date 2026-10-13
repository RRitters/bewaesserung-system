package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.SensorMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeasurementRepository extends JpaRepository<SensorMeasurement, Long> {
    // Sortiert nun korrekt nach der Eigenschaft 'gemessenAm'
    List<SensorMeasurement> findAllByOrderByGemessenAmDesc();
}