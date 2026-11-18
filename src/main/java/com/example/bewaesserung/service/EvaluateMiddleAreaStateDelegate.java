package com.example.bewaesserung.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("evaluateMiddleAreaStateDelegate")
public class EvaluateMiddleAreaStateDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(">>> Ausgeführt: evaluateMiddleAreaStateDelegate (Instanz: " + execution.getProcessInstanceId() + ")");
    }
}
