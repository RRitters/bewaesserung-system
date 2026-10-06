package com.example.bewaesserung.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("ventilOeffnenDelegate")
public class VentilOeffnenDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println("[VENTIL] Ventil wird GEÖFFNET.");
    }
}