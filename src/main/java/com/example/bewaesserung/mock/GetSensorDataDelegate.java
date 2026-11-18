package com.example.bewaesserung.mock;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component("getSensorDataDelegate")
public class GetSensorDataDelegate implements JavaDelegate {

    @Autowired
    private BodenfeuchteSimulationMock simulationMock;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        int reihe = execution.hasVariable("aktuelleReihe") ? ((Number) execution.getVariable("aktuelleReihe")).intValue() : 1;
        int platz = execution.hasVariable("aktuellerPlatz") ? ((Number) execution.getVariable("aktuellerPlatz")).intValue() : 1;
        boolean ventilOffen = execution.hasVariable("ventilOffen") && Boolean.TRUE.equals(execution.getVariable("ventilOffen"));

        Map<String, Object> sim = simulationMock.simuliereSchritt(reihe, platz, ventilOffen);

        execution.setVariable("bodenFeuchte10", sim.get("feuchte10"));
        execution.setVariable("bodenFeuchte35", sim.get("feuchte35"));
        execution.setVariable("bodenZuTrocken", sim.get("bodenZuTrocken"));

        System.out.println(String.format(">>> [MOCK-SENSOR] Reihe %d, Platz %d | 10cm: %.1f%% | 35cm: %.1f%% | Schließen-Prio (Trocken: %s) | Zähler [Offen: %d, Zu: %d]",
                reihe, platz,
                (double) sim.get("feuchte10"),
                (double) sim.get("feuchte35"),
                sim.get("bodenZuTrocken"),
                sim.get("totalOeffnungen"),
                sim.get("totalSchliessungen")));
    }
}