package com.example.bewaesserung.model;

public class ArduinoDataDto {
    private String nodeIp;      // z.B. "ARDUROW01"
    private Integer modbusId;   // z.B. 1
    private Double wert;        // z.B. 42.5

    public String getNodeIp() { return nodeIp; }
    public void setNodeIp(String nodeIp) { this.nodeIp = nodeIp; }

    public Integer getModbusId() { return modbusId; }
    public void setModbusId(Integer modbusId) { this.modbusId = modbusId; }

    public Double getWert() { return wert; }
    public void setWert(Double wert) { this.wert = wert; }
}