package com.example.bewaesserung.model;

import jakarta.persistence.*;

@Entity
@Table(name = "AKTOREN")
public class Aktor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AKTOR_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "TYP")
    private String typ;

    @Column(name = "NODE_IP")
    private String nodeIp;

    @Column(name = "MODBUS_ID")
    private Integer modbusId;

    @Column(name = "MODBUS_REGISTER")
    private String modbusRegister;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "BEMERKUNG")
    private String bemerkung;

    // Getter und Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTyp() { return typ; }
    public void setTyp(String typ) { this.typ = typ; }

    public String getNodeIp() { return nodeIp; }
    public void setNodeIp(String nodeIp) { this.nodeIp = nodeIp; }

    public Integer getModbusId() { return modbusId; }
    public void setModbusId(Integer modbusId) { this.modbusId = modbusId; }

    public String getModbusRegister() { return modbusRegister; }
    public void setModbusRegister(String modbusRegister) { this.modbusRegister = modbusRegister; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBemerkung() { return bemerkung; }
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }
}