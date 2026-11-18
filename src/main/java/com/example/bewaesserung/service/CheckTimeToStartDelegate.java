package com.example.bewaesserung.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("checkTimeToStartDelegate")
public class CheckTimeToStartDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Hier kommt später deine Logik rein (z.B. Uhrzeit prüfen, Sensoren abfragen)
        boolean sollStarten = true;

        // Ergebnis als Prozessvariable an Camunda übergeben
        execution.setVariable("sollStarten", sollStarten);
    }
}
