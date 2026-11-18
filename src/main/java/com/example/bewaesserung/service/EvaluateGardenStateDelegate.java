package com.example.bewaesserung.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("evaluateGardenStateDelegate")
public class EvaluateGardenStateDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(">>> Ausgeführt: evaluateGardenStateDelegate -> setzt isGarden = false");
        execution.setVariable("isGarden", false);
    }
}
