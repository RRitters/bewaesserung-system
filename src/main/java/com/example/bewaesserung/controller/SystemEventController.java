package com.example.bewaesserung.controller;

import com.example.bewaesserung.model.SystemEventLog;
import com.example.bewaesserung.repository.SystemEventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class SystemEventController {

    @Autowired
    private SystemEventLogRepository eventLogRepository;

    // Ruft alle aktiven, nicht quittierten Störungen ab (neueste zuerst)
    @GetMapping("/active")
    public List<SystemEventLog> getActiveEvents() {
        return eventLogRepository.findByAcknowledgedFalseOrderByTimestampDesc();
    }
}