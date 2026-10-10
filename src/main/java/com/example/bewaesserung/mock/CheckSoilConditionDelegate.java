package com.example.bewaesserung.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("checkSoilCondition")
public class CheckSoilConditionDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Wir holen die Feuchtigkeit, die der Sensor-Schritt eben gesetzt hat
        Double feuchte10cm = (Double) execution.getVariable("bodenfeuchte10cm");

        // Mock-Logik: Wenn kein Wert da ist, standardmäßig trocken annehmen
        boolean zuTrocken = true;

        if (feuchte10cm != null) {
            // Beispiel-Schwelle: Unter 25% muss gegossen werden
            // (Hier könnte man später die historischen Werte der letzten Tage einbeziehen)
            zuTrocken = (feuchte10cm < 25.0);
        }

        // Wir schreiben eine Prozessvariable 'bodenZuTrocken' (true oder false),
        // auf die das BPMN-Gateway (die Raute) hören kann.
        execution.setVariable("bodenZuTrocken", zuTrocken);

        System.out.printf("[GATEWAY MOCK] Bodenfeuchte 10cm: %.1f%% -> Boden zu trocken? %s%n",
                feuchte10cm != null ? feuchte10cm : 0.0, zuTrocken ? "JA (Ventil öffnen)" : "NEIN (Überspringen)");
    }
}