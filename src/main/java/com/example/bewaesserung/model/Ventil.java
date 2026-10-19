package com.example.bewaesserung.model;
//package com.deinpaket.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "ventile")
public class Ventil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VENTIL_ID")
    private Long id;

    private String name;

    // Z.B. "KUGELHAHN_ZEITGESTEUERT"
    private String typ;

    // Die Zeit in Millisekunden, die das Ventil von 0% (zu) auf 100% (auf) braucht
    @Column(name = "LAUFZEIT_MS")
    private Integer laufzeitMs;

    // Steuer-Parameter (0 - 100%)
    @Column(name = "SOLL_POSITION")
    private Integer sollPosition;

    @Column(name = "IST_POSITION")
    private Integer istPosition;

    // Hardware-Anbindung (Modbus oder direkte Pins am Arduino)
    private Integer modbusId;
    @Column(name = "REGISTER_AUF")
    private Integer registerAuf; // Relais 1 zieht an -> Ventil öffnet
    @Column(name = "REGISTER_ZU")
    private Integer registerZu;  // Relais 2 zieht an -> Ventil schließt

    @ManyToOne
    @JoinColumn(name = "platz_id")
    @JsonIgnore
    private Platz platz;

    // Zugehörigkeit zum Platz
//    @ManyToOne
//    @JoinColumn(name = "platz_id")
//    @JsonBackReference
//    private Platz platz;

    // --- Getter und Setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTyp() { return typ; }
    public void setTyp(String typ) { this.typ = typ; }

    public Integer getLaufzeitMs() { return laufzeitMs; }
    public void setLaufzeitMs(Integer laufzeitMs) { this.laufzeitMs = laufzeitMs; }

    public Integer getSollPosition() { return sollPosition; }
    public void setSollPosition(Integer sollPosition) { this.sollPosition = sollPosition; }

    public Integer getIstPosition() { return istPosition; }
    public void setIstPosition(Integer istPosition) { this.istPosition = istPosition; }

    public Integer getModbusId() { return modbusId; }
    public void setModbusId(Integer modbusId) { this.modbusId = modbusId; }

    public Integer getRegisterAuf() { return registerAuf; }
    public void setRegisterAuf(Integer registerAuf) { this.registerAuf = registerAuf; }

    public Integer getRegisterZu() { return registerZu; }
    public void setRegisterZu(Integer registerZu) { this.registerZu = registerZu; }

    public Platz getPlatz() { return platz; }
    public void setPlatz(Platz platz) { this.platz = platz; }
}