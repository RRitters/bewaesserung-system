package com.example.bewaesserung.repository;

import com.example.bewaesserung.entity.PlatzSteuerung;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlatzSteuerungRepository extends JpaRepository<PlatzSteuerung, Long> {
    List<PlatzSteuerung> findByZyklusStatus(String zyklusStatus);
}
