package com.example.bewaesserung.service;

import com.example.bewaesserung.model.Aktor;
import com.example.bewaesserung.model.Ventil;
import com.example.bewaesserung.repository.AktorRepository;
import com.example.bewaesserung.repository.VentilRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HardwareSteuerungsService {

    private static final Logger log = LoggerFactory.getLogger(HardwareSteuerungsService.class);

    // Sicherheitsabstand zwischen zwei Relais-Schaltvorgängen (verhindert Spannungseinbruch auf 48V)
    private static final long SCHALT_PAUSE_MS = 1500L;

    private final VentilRepository ventilRepository;
    private final AktorRepository aktorRepository;

    public HardwareSteuerungsService(VentilRepository ventilRepository, AktorRepository aktorRepository) {
        this.ventilRepository = ventilRepository;
        this.aktorRepository = aktorRepository;
    }

    /**
     * Öffnet ein einzelnes Ventil seriell und aktualisiert den DB-Status.
     */
    public synchronized boolean oeffneVentil(Ventil ventil) {
        if ("OPEN".equalsIgnoreCase(ventil.getStatus()) && Integer.valueOf(100).equals(ventil.getIstPosition())) {
            log.info("Ventil {} (ID {}) ist bereits OPEN.", ventil.getName(), ventil.getId());
            return true;
        }

        try {
            log.info("Öffne Ventil {} (Modbus-ID: {}, Register AUF: {})...",
                    ventil.getName(), ventil.getModbusId(), ventil.getRegisterAuf());

            ventil.setStatus("OPENING");
            ventil.setSollPosition(100);
            ventil.setLetzteAktion(LocalDateTime.now());
            ventilRepository.save(ventil);

            // TODO: Hier deinen bestehenden Modbus-/ESP32-Aufruf einhängen:
            // sendeModbusBefehl(ventil.getModbusId(), ventil.getRegisterAuf(), true);

            // Laufzeit abwarten (oder non-blocking über Status-Timer, falls gewünscht)
            int laufzeit = ventil.getLaufzeitMs() != null ? ventil.getLaufzeitMs() : 10000;
            Thread.sleep(laufzeit);

            // Relais nach Laufzeit wieder abfallen lassen
            // sendeModbusBefehl(ventil.getModbusId(), ventil.getRegisterAuf(), false);

            ventil.setIstPosition(100);
            ventil.setStatus("OPEN");
            ventil.setLetzteAktion(LocalDateTime.now());
            ventilRepository.save(ventil);

            // Kurze Beruhigungspause für das 48V-Netzteil vor dem nächsten Ventil
            Thread.sleep(SCHALT_PAUSE_MS);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ventil.setStatus("UNKNOWN");
            ventilRepository.save(ventil);
            return false;
        } catch (Exception e) {
            log.error("Fehler beim Öffnen von Ventil {}: {}", ventil.getId(), e.getMessage());
            ventil.setStatus("UNKNOWN");
            ventilRepository.save(ventil);
            return false;
        }
    }

    /**
     * Schließt ein einzelnes Ventil seriell und aktualisiert den DB-Status.
     */
    public synchronized boolean schliesseVentil(Ventil ventil) {
        if ("CLOSED".equalsIgnoreCase(ventil.getStatus()) && Integer.valueOf(0).equals(ventil.getIstPosition())) {
            log.info("Ventil {} (ID {}) ist bereits CLOSED.", ventil.getName(), ventil.getId());
            return true;
        }

        try {
            log.info("Schließe Ventil {} (Modbus-ID: {}, Register ZU: {})...",
                    ventil.getName(), ventil.getModbusId(), ventil.getRegisterZu());

            ventil.setStatus("CLOSING");
            ventil.setSollPosition(0);
            ventil.setLetzteAktion(LocalDateTime.now());
            ventilRepository.save(ventil);

            // TODO: Hier deinen bestehenden Modbus-/ESP32-Aufruf einhängen:
            // sendeModbusBefehl(ventil.getModbusId(), ventil.getRegisterZu(), true);

            int laufzeit = ventil.getLaufzeitMs() != null ? ventil.getLaufzeitMs() : 10000;
            Thread.sleep(laufzeit + 500); // +500ms Reserve für sicheren Endanschlag

            // sendeModbusBefehl(ventil.getModbusId(), ventil.getRegisterZu(), false);

            ventil.setIstPosition(0);
            ventil.setStatus("CLOSED");
            ventil.setLetzteAktion(LocalDateTime.now());
            ventilRepository.save(ventil);

            Thread.sleep(SCHALT_PAUSE_MS);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ventil.setStatus("UNKNOWN");
            ventilRepository.save(ventil);
            return false;
        } catch (Exception e) {
            log.error("Fehler beim Schließen von Ventil {}: {}", ventil.getId(), e.getMessage());
            ventil.setStatus("UNKNOWN");
            ventilRepository.save(ventil);
            return false;
        }
    }

    /**
     * Fährt alle Ventile eines Bereichs oder des gesamten Systems NACHEINANDER sicher zu.
     * Wird z. B. beim Setup oder Not-Halt genutzt (niemals gleichzeitig!).
     */
    public synchronized boolean alleVentileSeriellSchliessen(List<Ventil> ventile) {
        boolean alleOk = true;
        for (Ventil v : ventile) {
            if (!"CLOSED".equalsIgnoreCase(v.getStatus())) {
                boolean ok = schliesseVentil(v);
                if (!ok) {
                    alleOk = false;
                }
            }
        }
        return alleOk;
    }

    /**
     * Schaltet einen Schaltschrank-Aktor (z. B. 48V Pumpe, 2"-Hauptventil, Refill-Pumpe).
     */
    public synchronized boolean schalteAktor(String aktorName, boolean einschalten) {
        Aktor aktor = aktorRepository.findByName(aktorName)
                .orElseThrow(() -> new IllegalStateException("Aktor nicht in DB gefunden: " + aktorName));

        log.info("Schalte Aktor '{}' (IP: {}, Modbus-ID: {}, Reg: {}) -> {}",
                aktor.getName(), aktor.getNodeIp(), aktor.getModbusId(), aktor.getModbusRegister(),
                einschalten ? "ON" : "OFF");

        // TODO: Modbus/HTTP-Befehl an aktor.getNodeIp() absetzen

        aktor.setStatus(einschalten ? "ON" : "OFF");
        aktorRepository.save(aktor);
        return true;
    }
}