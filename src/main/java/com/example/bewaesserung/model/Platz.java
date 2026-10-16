package com.example.bewaesserung.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PLATZ")
public class Platz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // z. B. "Reihe 1, Platz 5" oder "R1-P5"
    @Column(unique = true, nullable = false)
    private String name;

    // 0 = Leer, 1 bis 3 = Bäume vorhanden
    private int anzahlBaeume;

    // Um später das Alter zu berechnen (relevant für die Bewässerungsmenge/Blüte)
    private LocalDate pflanzDatum;

    // true = Baum wird auf Blüte vorbereitet (Trockenstress zulassen)
    // false = normales Wachstum
    private boolean inBluetenPhase;

    // Der Platz kennt alle seine Sensoren
    @OneToMany
    @JoinColumn(name = "platz_id") // Sucht in der Sensor-Tabelle nach dieser ID
    private List<Sensor> sensoren = new ArrayList<>();

    public Platz() {}

    public Platz(String name, int anzahlBaeume, LocalDate pflanzDatum, boolean inBluetenPhase) {
        this.name = name;
        this.anzahlBaeume = anzahlBaeume;
        this.pflanzDatum = pflanzDatum;
        this.inBluetenPhase = inBluetenPhase;
    }

    // --- Getter und Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAnzahlBaeume() { return anzahlBaeume; }
    public void setAnzahlBaeume(int anzahlBaeume) { this.anzahlBaeume = anzahlBaeume; }

    public LocalDate getPflanzDatum() { return pflanzDatum; }
    public void setPflanzDatum(LocalDate pflanzDatum) { this.pflanzDatum = pflanzDatum; }

    public boolean isInBluetenPhase() { return inBluetenPhase; }
    public void setInBluetenPhase(boolean inBluetenPhase) { this.inBluetenPhase = inBluetenPhase; }
    // ... (und ganz unten die Getter/Setter dafür einfügen:)
    public List<Sensor> getSensoren() { return sensoren; }
    public void setSensoren(List<Sensor> sensoren) { this.sensoren = sensoren; }

}