package com.example.bewaesserung.repository;

import com.example.bewaesserung.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {
    // Diese Zeile ist neu:
    Optional<Sensor> findByNodeIpAndModbusId(String nodeIp, Integer modbusId);
}