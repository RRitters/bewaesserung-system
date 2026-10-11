package com.example.bewaesserung.mock;

import com.example.bewaesserung.model.SystemEventLog;
import com.example.bewaesserung.repository.SystemEventLogRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("getSensorDataDelegate")
public class GetSensorDataDelegate implements JavaDelegate {

    @Autowired
    private SystemEventLogRepository eventLogRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Beispiel-Werte (später kommen hier deine echten Hardware-Werte hin)
        double soilMoisture = 45.0; // Angenommener Messwert in %
        boolean tankLow = false;    // Tank-Status

        // 1. Normale Prozessvariable für das BPMN-Gateway setzen
        boolean bodenZuTrocken = (soilMoisture < 30.0);
        execution.setVariable("bodenZuTrocken", bodenZuTrocken);

        // 2. Überprüfung auf kritische Alarme (z.B. Überwässerung / Feuchte zu hoch)
        if (soilMoisture > 85.0) {
            SystemEventLog alert = new SystemEventLog(
                    "FEHLER",
                    "BEWAESSERUNG",
                    "Reihe1_Platz1",
                    "Kritische Überwässerung! Feuchte bei " + soilMoisture + "%"
            );
            eventLogRepository.save(alert);
        }

        // 3. Überprüfung auf Tank-Status
        if (tankLow) {
            SystemEventLog warning = new SystemEventLog(
                    "WARNUNG",
                    "TANK",
                    "System",
                    "Wassertank-Level ist LOW! Pumpe blockiert."
            );
            eventLogRepository.save(warning);
        }
    }
}