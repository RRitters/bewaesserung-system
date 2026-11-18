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

    // Bereich: "DURIAN", "GARDEN", "MIDDLE_AREA"
    @Column(name = "BEREICH")
    private String bereich;

    // Priorität für die Queue (1 = höchste Priorität, 5 = niedrigste)
    @Column(name = "PRIORITAET")
    private Integer prioritaet = 3;

    // Gewünschte Wassermenge in Litern für den aktuellen Zyklus
    @Column(name = "SOLL_LITER")
    private Double sollLiter = 0.0;

    // Bisher abgeflossene Liter in diesem Zyklus
    @Column(name = "IST_LITER")
    private Double istLiter = 0.0;

    // Status in der Queue: "IDLE", "QUEUED", "WATERING", "FINISHED"
    @Column(name = "QUEUE_STATUS")
    private String queueStatus = "IDLE";

    // Der Platz kennt alle seine Sensoren (Unidirektional über JoinColumn)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "platz_id")
    private List<Sensor> sensoren = new ArrayList<>();

    // Der Platz kennt alle seine Ventile (Bidirektional über mappedBy)
    @OneToMany(mappedBy = "platz", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Ventil> ventile = new ArrayList<>();

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

    public String getBereich() { return bereich; }
    public void setBereich(String bereich) { this.bereich = bereich; }

    public Integer getPrioritaet() { return prioritaet; }
    public void setPrioritaet(Integer prioritaet) { this.prioritaet = prioritaet; }

    public Double getSollLiter() { return sollLiter; }
    public void setSollLiter(Double sollLiter) { this.sollLiter = sollLiter; }

    public Double getIstLiter() { return istLiter; }
    public void setIstLiter(Double istLiter) { this.istLiter = istLiter; }

    public String getQueueStatus() { return queueStatus; }
    public void setQueueStatus(String queueStatus) { this.queueStatus = queueStatus; }

    public List<Sensor> getSensoren() { return sensoren; }
    public void setSensoren(List<Sensor> sensoren) { this.sensoren = sensoren; }

    public List<Ventil> getVentile() {
        return ventile;
    }

    public void setVentile(List<Ventil> ventile) {
        this.ventile = ventile;
        if (this.ventile != null) {
            for (Ventil v : this.ventile) {
                v.setPlatz(this);
            }
        }
    }
}