package com.example.bewaesserung.delegate;

import com.example.bewaesserung.model.Platz;
import com.example.bewaesserung.model.Ventil;
import com.example.bewaesserung.repository.PlatzRepository;
import com.example.bewaesserung.service.HardwareSteuerungsService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 2. Schritt in der Schleife: "Close Valve on ready place Max x"
 */
@Component("closeReadyValvesDelegate")
public class CloseReadyValvesDelegate implements JavaDelegate {

    private final PlatzRepository platzRepository;
    private final HardwareSteuerungsService hardwareService;

    public CloseReadyValvesDelegate(PlatzRepository platzRepository, HardwareSteuerungsService hardwareService) {
        this.platzRepository = platzRepository;
        this.hardwareService = hardwareService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        List<Long> zuSchliessendeIds = (List<Long>) execution.getVariable("zuSchliessendePlatzIds");
        if (zuSchliessendeIds == null || zuSchliessendeIds.isEmpty()) {
            return;
        }

        for (Long platzId : zuSchliessendeIds) {
            platzRepository.findById(platzId).ifPresent(platz -> {
                for (Ventil v : platz.getVentile()) {
                    hardwareService.schliesseVentil(v);
                }
                platz.setQueueStatus("FINISHED");
                platzRepository.save(platz);
            });
        }
    }
}