package com.example.bewaesserung.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "SYSTEM_EVENT_LOG")
public class SystemEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Millisekundengenauer Zeitstempel
    @Column(nullable = false)
    private Instant timestamp;

    // Kategorie: INFO, WARNUNG, FEHLER
    @Column(length = 50)
    private String category;

    // Gruppe: BEWAESSERUNG, PUMPE, TANK, SENSORIK
    @Column(length = 50)
    private String groupName;

    // Welcher Platz / Welche Reihe betrifft es (z.B. "Reihe3_Platz5")
    @Column(length = 50)
    private String plotId;

    // Die genaue Fehlermeldung
    @Column(length = 500)
    private String message;

    // Ob der Fehler im Dashboard bereits quittiert wurde
    private boolean acknowledged = false;

    // Konstruktoren
    public SystemEventLog() {
        this.timestamp = Instant.now(); // Setzt den exakten Zeitstempel beim Erstellen auf ms genau
    }

    public SystemEventLog(String category, String groupName, String plotId, String message) {
        this.timestamp = Instant.now();
        this.category = category;
        this.groupName = groupName;
        this.plotId = plotId;
        this.message = message;
    }

    // Getter und Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getPlotId() { return plotId; }
    public void setPlotId(String plotId) { this.plotId = plotId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
}