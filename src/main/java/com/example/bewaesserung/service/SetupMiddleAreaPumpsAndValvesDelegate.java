package com.example.bewaesserung.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("setupMiddleAreaPumpsAndValvesDelegate")
public class SetupMiddleAreaPumpsAndValvesDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(">>> Ausgeführt: setupMiddleAreaPumpsAndValvesDelegate -> setzt setupOk = true");
        execution.setVariable("setupOk", true);
    }
}
