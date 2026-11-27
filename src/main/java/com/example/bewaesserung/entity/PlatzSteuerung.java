package com.example.bewaesserung.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PLATZ_STEUERUNG", schema = "BEWAESSERUNG")
public class PlatzSteuerung {

    @Id
    @Column(name = "PLATZ_ID", nullable = false)
    private Long platzId;

    @Column(name = "SOLL_LITER", precision = 8, scale = 2, nullable = false)
    private BigDecimal sollLiter = BigDecimal.ZERO;

    @Column(name = "IST_LITER", precision = 8, scale = 2, nullable = false)
    private BigDecimal istLiter = BigDecimal.ZERO;

    @Column(name = "PULS_SOLL_LITER", precision = 8, scale = 2, nullable = false)
    private BigDecimal pulsSollLiter = BigDecimal.ZERO;

    @Column(name = "NAECHSTE_ABSCHALTUNG_LITER", precision = 8, scale = 2, nullable = false)
    private BigDecimal naechsteAbschaltungLiter = BigDecimal.ZERO;

    @Column(name = "EINSICKERPAUSE_SOLL_MIN", precision = 6, scale = 2, nullable = false)
    private BigDecimal einsickerpauseSollMin = BigDecimal.valueOf(15);

    @Column(name = "EINSICKERPAUSE_IST_MIN", precision = 6, scale = 2, nullable = false)
    private BigDecimal einsickerpauseIstMin = BigDecimal.ZERO;

    @Column(name = "ANZAHL_BEWAESSERUNG", nullable = false)
    private Integer anzahlBewaesserung = 0;

    @Column(name = "ANZAHL_PAUSE", nullable = false)
    private Integer anzahlPause = 0;

    @Column(name = "SOLL_DURCHFLUSS_LPM", precision = 6, scale = 2, nullable = false)
    private BigDecimal sollDurchflussLpm = BigDecimal.ZERO;

    @Column(name = "ZYKLUS_STATUS", length = 20, nullable = false)
    private String zyklusStatus = "BEREIT";

    @Column(name = "AKTUALISIERT_AM", insertable = false, updatable = false)
    private LocalDateTime aktualisiertAm;

    // Getter & Setter
    public Long getPlatzId() { return platzId; }
    public void setPlatzId(Long platzId) { this.platzId = platzId; }

    public BigDecimal getSollLiter() { return sollLiter; }
    public void setSollLiter(BigDecimal sollLiter) { this.sollLiter = sollLiter; }

    public BigDecimal getIstLiter() { return istLiter; }
    public void setIstLiter(BigDecimal istLiter) { this.istLiter = istLiter; }

    public BigDecimal getPulsSollLiter() { return pulsSollLiter; }
    public void setPulsSollLiter(BigDecimal pulsSollLiter) { this.pulsSollLiter = pulsSollLiter; }

    public BigDecimal getNaechsteAbschaltungLiter() { return naechsteAbschaltungLiter; }
    public void setNaechsteAbschaltungLiter(BigDecimal naechsteAbschaltungLiter) { this.naechsteAbschaltungLiter = naechsteAbschaltungLiter; }

    public BigDecimal getEinsickerpauseSollMin() { return einsickerpauseSollMin; }
    public void setEinsickerpauseSollMin(BigDecimal einsickerpauseSollMin) { this.einsickerpauseSollMin = einsickerpauseSollMin; }

    public BigDecimal getEinsickerpauseIstMin() { return einsickerpauseIstMin; }
    public void setEinsickerpauseIstMin(BigDecimal einsickerpauseIstMin) { this.einsickerpauseIstMin = einsickerpauseIstMin; }

    public Integer getAnzahlBewaesserung() { return anzahlBewaesserung; }
    public void setAnzahlBewaesserung(Integer anzahlBewaesserung) { this.anzahlBewaesserung = anzahlBewaesserung; }

    public Integer getAnzahlPause() { return anzahlPause; }
    public void setAnzahlPause(Integer anzahlPause) { this.anzahlPause = anzahlPause; }

    public BigDecimal getSollDurchflussLpm() { return sollDurchflussLpm; }
    public void setSollDurchflussLpm(BigDecimal sollDurchflussLpm) { this.sollDurchflussLpm = sollDurchflussLpm; }

    public String getZyklusStatus() { return zyklusStatus; }
    public void setZyklusStatus(String zyklusStatus) { this.zyklusStatus = zyklusStatus; }

    public LocalDateTime getAktualisiertAm() { return aktualisiertAm; }
}
