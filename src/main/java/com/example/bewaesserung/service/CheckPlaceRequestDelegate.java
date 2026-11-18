package com.example.bewaesserung.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("checkPlaceRequestDelegate")
public class CheckPlaceRequestDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(">>> Ausgeführt: checkPlaceRequestDelegate -> setzt hasRequest = true");
        execution.setVariable("hasRequest", true);
    }
}
