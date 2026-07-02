package com.openbake.server.controller;

import com.openbake.server.config.AppProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mirrors main.py's "/health" DB-ping liveness probe.
 * The bare "/" root now serves the embedded web app's index.html instead of
 * the old JSON status message, since this jar serves both API and web UI.
 */
@RestController
public class HealthController {

    @PersistenceContext
    private EntityManager entityManager;

    private final AppProperties appProperties;

    public HealthController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbOk;
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            dbOk = true;
        } catch (Exception e) {
            dbOk = false;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", dbOk ? "healthy" : "degraded");
        body.put("database", dbOk ? "ok" : "unreachable");
        body.put("version", "1.0.0");
        body.put("env", appProperties.getEnv());

        return ResponseEntity.status(dbOk ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
