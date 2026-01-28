package com.eventhub.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para el endpoint raíz del API.
 * Proporciona información básica y enlaces a la documentación.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "EventHub API");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("description", "Event-Driven Cloud Application");

        Map<String, String> links = new HashMap<>();
        links.put("health", "/actuator/health");
        links.put("swagger", "/swagger-ui.html");
        links.put("api-docs", "/api-docs");
        links.put("events", "/api/events");

        response.put("_links", links);

        return ResponseEntity.ok(response);
    }
}
