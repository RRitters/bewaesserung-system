package com.example.bewaesserung.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("openValve")
public class OpenValveDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println("[VENTIL] Boden ist zu trocken! Ventil wird JETZT GEÖFFNET.");
        // Hier kommt später der Code für das echte Arduino/Relais hin
    }
}package com.example.bewaesserung.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("openValve")
public class OpenValveDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println("[VENTIL] Boden ist zu trocken! Ventil wird JETZT GEÖFFNET.");
        // Hier kommt später der Code für das echte Arduino/Relais hin
    }
}