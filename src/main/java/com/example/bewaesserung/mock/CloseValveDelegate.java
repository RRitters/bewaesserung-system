package com.example.bewaesserung.mock;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("closeValve")
public class CloseValveDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println("[VENTIL] Gießzeit abgelaufen oder Boden feucht genug! Ventil wird JETZT GESCHLOSSEN.");
        // Hier kommt später der Code für das Schließen des Arduino-Relais hin
    }
}