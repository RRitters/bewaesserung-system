package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.Messwert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesswertRepository extends JpaRepository<Messwert, Long> {
    List<Messwert> findBySensorIdOrderByGemessenAmDesc(Long sensorId);
}