package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.Ventil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentilRepository extends JpaRepository<Ventil, Long> {
}