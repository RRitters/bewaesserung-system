package com.example.bewaesserung.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("stopMiddleAreaPumpsAndValvesDelegate")
public class StopMiddleAreaPumpsAndValvesDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(">>> Ausgeführt: stopMiddleAreaPumpsAndValvesDelegate (Instanz: " + execution.getProcessInstanceId() + ")");
    }
}
