package com.example.bewaesserung.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "MESSWERTE", schema = "SYSTEM") // Oder das Schema eintragen, in dem sie liegt
public class SensorMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MESSWERT_ID")
    private Long id;

    @Column(name = "SENSOR_ID")
    private Long sensorId;

    @Column(name = "WERT")
    private double wert;

    @Column(name = "EINHEIT")
    private String einheit;

    @Column(name = "GEMESSEN_AM")
    private LocalDateTime gemessenAm = LocalDateTime.now();

    // Standard-Konstruktor
    public SensorMeasurement() {}

    // Getter und Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSensorId() { return sensorId; }
    public void setSensorId(Long sensorId) { this.sensorId = sensorId; }

    public double getWert() { return wert; }
    public void setWert(double wert) { this.wert = wert; }

    public String getEinheit() { return einheit; }
    public void setEinheit(String einheit) { this.einheit = einheit; }

    public LocalDateTime getGemessenAm() { return gemessenAm; }
    public void setGemessenAm(LocalDateTime gemessenAm) { this.gemessenAm = gemessenAm; }
}