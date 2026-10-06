package com.example.bewaesserung.mock;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component("getSensorDataDelegate")
public class GetSensorDataDelegate implements JavaDelegate {

    private final Random random = new Random();

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        double bodenfeuchte35cm = 20.0 + (30.0 * random.nextDouble());
        double bodenfeuchte10cm = 15.0 + (35.0 * random.nextDouble());
        double bodentemperatur10cm = 2.0 + (20.0 * random.nextDouble());

        execution.setVariable("bodenfeuchte35cm", bodenfeuchte35cm);
        execution.setVariable("bodenfeuchte10cm", bodenfeuchte10cm);
        execution.setVariable("bodentemperatur10cm", bodentemperatur10cm);

        System.out.printf("[SENSOR MOCK] Feuchte 35cm: %.1f%% | Feuchte 10cm: %.1f%% | Temp 10cm: %.1f°C%n",
                bodenfeuchte35cm, bodenfeuchte10cm, bodentemperatur10cm);
    }
}