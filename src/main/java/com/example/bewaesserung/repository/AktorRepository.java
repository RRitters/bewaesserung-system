package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.Aktor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AktorRepository extends JpaRepository<Aktor, Long> {

    Optional<Aktor> findByName(String name);
}