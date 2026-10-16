package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.Aktor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AktorRepository extends JpaRepository<Aktor, Long> {
}