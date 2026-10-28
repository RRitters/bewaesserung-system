package com.example.bewaesserung.dto;

import java.util.List;

public class PlatzConfigDto {
    private Long platzId;
    private String platzName;
    private List<Integer> sensorModbusIds;

    // Neue Ventil-Parameter für das Modbus-Relais (Kugelhahn)
    private int ventilModbusId;
    private int ventilLaufzeitMs;
    private int sollPosition; // z.B. 0-100%

    public PlatzConfigDto() {}

    public Long getPlatzId() { return platzId; }
    public void setPlatzId(Long platzId) { this.platzId = platzId; }

    public String getPlatzName() { return platzName; }
    public void setPlatzName(String platzName) { this.platzName = platzName; }

    public List<Integer> getSensorModbusIds() { return sensorModbusIds; }
    public void setSensorModbusIds(List<Integer> sensorModbusIds) { this.sensorModbusIds = sensorModbusIds; }

    public int getVentilModbusId() { return ventilModbusId; }
    public void setVentilModbusId(int ventilModbusId) { this.ventilModbusId = ventilModbusId; }

    public int getVentilLaufzeitMs() { return ventilLaufzeitMs; }
    public void setVentilLaufzeitMs(int ventilLaufzeitMs) { this.ventilLaufzeitMs = ventilLaufzeitMs; }

    public int getSollPosition() { return sollPosition; }
    public void setSollPosition(int sollPosition) { this.sollPosition = sollPosition; }
}