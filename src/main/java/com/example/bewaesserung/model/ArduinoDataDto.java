package com.example.bewaesserung.model;

public class ArduinoDataDto {
    private String arduinoId;
    private Long plotId;
    private Double soilMoisture;
    private Double tankLevel;

    // Getter und Setter
    public String getArduinoId() { return arduinoId; }
    public void setArduinoId(String arduinoId) { this.arduinoId = arduinoId; }

    public Long getPlotId() { return plotId; }
    public void setPlotId(Long plotId) { this.plotId = plotId; }

    public Double getSoilMoisture() { return soilMoisture; }
    public void setSoilMoisture(Double soilMoisture) { this.soilMoisture = soilMoisture; }

    public Double getTankLevel() { return tankLevel; }
    public void setTankLevel(Double tankLevel) { this.tankLevel = tankLevel; }
}