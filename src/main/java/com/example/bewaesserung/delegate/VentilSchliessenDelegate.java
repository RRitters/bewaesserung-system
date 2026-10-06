package com.example.bewaesserung.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("ventilSchliessenDelegate")
public class VentilSchliessenDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println("[VENTIL] Ventil wird GESCHLOSSEN.");
    }
}
