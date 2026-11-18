package com.example.bewaesserung.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("evaluateDurianStateDelegate")
public class EvaluateDurianStateDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(">>> Ausgeführt: evaluateDurianStateDelegate (Instanz: " + execution.getProcessInstanceId() + ")");
    }
}
