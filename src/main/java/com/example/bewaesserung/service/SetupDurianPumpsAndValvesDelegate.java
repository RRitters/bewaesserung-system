package com.example.bewaesserung.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("setupDurianPumpsAndValvesDelegate")
public class SetupDurianPumpsAndValvesDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(">>> Ausgeführt: setupDurianPumpsAndValvesDelegate (Instanz: " + execution.getProcessInstanceId() + ")");
    }
}
