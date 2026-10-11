package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.SystemEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemEventLogRepository extends JpaRepository<SystemEventLog, Long> {

    // Hilfreich für das Dashboard: Holt alle nicht quittierten Fehler, sortiert nach Zeit (neueste zuerst)
    List<SystemEventLog> findByAcknowledgedFalseOrderByTimestampDesc();
}