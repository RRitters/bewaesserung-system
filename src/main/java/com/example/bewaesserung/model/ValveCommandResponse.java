package com.example.bewaesserung.model;

public class ValveCommandResponse {
    private Long plotId;
    private boolean openValve;
    private String message;

    public ValveCommandResponse(Long plotId, boolean openValve, String message) {
        this.plotId = plotId;
        this.openValve = openValve;
        this.message = message;
    }

    // Getter und Setter
    public Long getPlotId() { return plotId; }
    public void setPlotId(Long plotId) { this.plotId = plotId; }

    public boolean isopenValve() { return openValve; }
    public void setopenValve(boolean openValve) { this.openValve = openValve; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}