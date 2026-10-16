package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.Platz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatzRepository extends JpaRepository<Platz, Long> {
}