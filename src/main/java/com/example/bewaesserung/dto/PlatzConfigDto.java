package com.example.bewaesserung.dto;

import java.util.List;

public class PlatzConfigDto {
    private Long platzId;
    private String platzName;
    private int ventilPin;
    private List<Integer> modbusIds;
    private double ventilKorrekturFaktor;
    private int letzteVentilStellung;

    // Standard Konstruktor
    public PlatzConfigDto() {}

    // Getter und Setter
    public Long getPlatzId() { return platzId; }
    public void setPlatzId(Long platzId) { this.platzId = platzId; }

    public String getPlatzName() { return platzName; }
    public void setPlatzName(String platzName) { this.platzName = platzName; }

    public int getVentilPin() { return ventilPin; }
    public void setVentilPin(int ventilPin) { this.ventilPin = ventilPin; }

    public List<Integer> getModbusIds() { return modbusIds; }
    public void setModbusIds(List<Integer> modbusIds) { this.modbusIds = modbusIds; }

    public double getVentilKorrekturFaktor() { return ventilKorrekturFaktor; }
    public void setVentilKorrekturFaktor(double ventilKorrekturFaktor) { this.ventilKorrekturFaktor = ventilKorrekturFaktor; }

    public int getLetzteVentilStellung() { return letzteVentilStellung; }
    public void setLetzteVentilStellung(int letzteVentilStellung) { this.letzteVentilStellung = letzteVentilStellung; }
}