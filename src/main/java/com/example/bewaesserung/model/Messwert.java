package com.example.bewaesserung.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "MESSWERTE")
public class Messwert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PLATZ_ID", nullable = false)
    private Long platzId;

    @Column(name = "GEMESSEN_AM")
    private LocalDateTime gemessenAm;

    // Kleinklima
    @Column(name = "LUFTTEMPERATUR")
    private Double luftTemperatur;

    @Column(name = "LUFTFEUCHTE")
    private Double luftFeuchte;

    // Bodendaten
    @Column(name = "BODENTEMPERATUR")
    private Double bodenTemperatur;

    @Column(name = "BODENFEUCHTE")
    private Double bodenFeuchte;

    @Column(name = "EC_WERT")
    private Double ecWert;

    @Column(name = "PH_WERT")
    private Double phWert;

    @Column(name = "STICKSTOFF")
    private Double stickstoff;

    @Column(name = "PHOSPHOR")
    private Double phosphor;

    @Column(name = "KALIUM")
    private Double kalium;

    @Column(name = "DURCHFLUSS")
    private Double durchfluss;

    // Getter und Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlatzId() { return platzId; }
    public void setPlatzId(Long platzId) { this.platzId = platzId; }

    public LocalDateTime getGemessenAm() { return gemessenAm; }
    public void setGemessenAm(LocalDateTime gemessenAm) { this.gemessenAm = gemessenAm; }

    public Double getLuftTemperatur() { return luftTemperatur; }
    public void setLuftTemperatur(Double luftTemperatur) { this.luftTemperatur = luftTemperatur; }

    public Double getLuftFeuchte() { return luftFeuchte; }
    public void setLuftFeuchte(Double luftFeuchte) { this.luftFeuchte = luftFeuchte; }

    public Double getBodenTemperatur() { return bodenTemperatur; }
    public void setBodenTemperatur(Double bodenTemperatur) { this.bodenTemperatur = bodenTemperatur; }

    public Double getBodenFeuchte() { return bodenFeuchte; }
    public void setBodenFeuchte(Double bodenFeuchte) { this.bodenFeuchte = bodenFeuchte; }

    public Double getEcWert() { return ecWert; }
    public void setEcWert(Double ecWert) { this.ecWert = ecWert; }

    public Double getPhWert() { return phWert; }
    public void setPhWert(Double phWert) { this.phWert = phWert; }

    public Double getStickstoff() { return stickstoff; }
    public void setStickstoff(Double stickstoff) { this.stickstoff = stickstoff; }

    public Double getPhosphor() { return phosphor; }
    public void setPhosphor(Double phosphor) { this.phosphor = phosphor; }

    public Double getKalium() { return kalium; }
    public void setKalium(Double kalium) { this.kalium = kalium; }

    public Double getDurchfluss() { return durchfluss; }
    public void setDurchfluss(Double durchfluss) { this.durchfluss = durchfluss; }
}