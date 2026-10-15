package com.example.bewaesserung.model;

import jakarta.persistence.*;

@Entity
@Table(name = "SENSOREN")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SENSOR_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "TYP")
    private String typ;

    @Column(name = "TIEFE_CM")
    private Integer tiefeCm;

    @Column(name = "EINHEIT")
    private String einheit;

    @Column(name = "NODE_IP")
    private String nodeIp;

    @Column(name = "MODBUS_ID")
    private Integer modbusId;

    @Column(name = "MODBUS_REGISTER")
    private String modbusRegister;

    @Column(name = "SOLL_MIN")
    private Double sollMin;

    @Column(name = "SOLL_MAX")
    private Double sollMax;

    @Column(name = "KRITISCH_MIN")
    private Double kritischMin;

    @Column(name = "KRITISCH_MAX")
    private Double kritischMax;

    @Column(name = "BEMERKUNG")
    private String bemerkung;

    // Standard-Konstruktor
    public Sensor() {}

    // Getter und Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTyp() { return typ; }
    public void setTyp(String typ) { this.typ = typ; }

    public Integer getTiefeCm() { return tiefeCm; }
    public void setTiefeCm(Integer tiefeCm) { this.tiefeCm = tiefeCm; }

    public String getEinheit() { return einheit; }
    public void setEinheit(String einheit) { this.einheit = einheit; }

    public String getNodeIp() { return nodeIp; }
    public void setNodeIp(String nodeIp) { this.nodeIp = nodeIp; }

    public Integer getModbusId() { return modbusId; }
    public void setModbusId(Integer modbusId) { this.modbusId = modbusId; }

    public String getModbusRegister() { return modbusRegister; }
    public void setModbusRegister(String modbusRegister) { this.modbusRegister = modbusRegister; }

    public Double getSollMin() { return sollMin; }
    public void setSollMin(Double sollMin) { this.sollMin = sollMin; }

    public Double getSollMax() { return sollMax; }
    public void setSollMax(Double sollMax) { this.sollMax = sollMax; }

    public Double getKritischMin() { return kritischMin; }
    public void setKritischMin(Double kritischMin) { this.kritischMin = kritischMin; }

    public Double getKritischMax() { return kritischMax; }
    public void setKritischMax(Double kritischMax) { this.kritischMax = kritischMax; }

    public String getBemerkung() { return bemerkung; }
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }
}