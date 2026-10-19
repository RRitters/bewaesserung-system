package com.example.bewaesserung.dto;

public class ArduinoAckDto {
    private String status;
    private int plaetzeKonfiguriert;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPlaetzeKonfiguriert() { return plaetzeKonfiguriert; }
    public void setPlaetzeKonfiguriert(int plaetzeKonfiguriert) { this.plaetzeKonfiguriert = plaetzeKonfiguriert; }
}