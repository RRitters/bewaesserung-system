package com.example.bewaesserung;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BewaesserungApplication {

    public static void main(String[] args) {
        SpringApplication.run(BewaesserungApplication.class, args);
    }

    @Bean
    public CommandLineRunner startProcess(RuntimeService runtimeService) {
        return args -> {
            System.out.println("==========================================");
            System.out.println("Starte Bewässerungsprozess...");
            System.out.println("==========================================");

            runtimeService.startProcessInstanceByKey("bewaesserung-prozess");
        };
    }
}