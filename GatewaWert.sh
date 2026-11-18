DIR="src/main/java/com/example/bewaesserung/service"

set_var() {
  local class=$1
  local bean=$2
  local varName=$3
  local val=$4
  cat <<EOF > "$DIR/${class}.java"
package com.example.bewaesserung.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("$bean")
public class ${class} implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(">>> Ausgeführt: $bean -> setzt $varName = $val");
        execution.setVariable("$varName", $val);
    }
}
EOF
}

set_var "CheckPlaceRequestDelegate" "checkPlaceRequestDelegate" "hasRequest" "true"
set_var "EvaluateDurianStateDelegate" "evaluateDurianStateDelegate" "isDurian" "true"
set_var "EvaluateGardenStateDelegate" "evaluateGardenStateDelegate" "isGarden" "false"
set_var "EvaluateMiddleAreaStateDelegate" "evaluateMiddleAreaStateDelegate" "isMiddleArea" "false"
set_var "SetupDurianPumpsAndValvesDelegate" "setupDurianPumpsAndValvesDelegate" "setupOk" "true"
set_var "SetupGardenPumpsAndValvesDelegate" "setupGardenPumpsAndValvesDelegate" "setupOk" "true"
set_var "SetupMiddleAreaPumpsAndValvesDelegate" "setupMiddleAreaPumpsAndValvesDelegate" "setupOk" "true"
