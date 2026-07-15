package com.smartlogix.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return buildResponse("El servicio de autenticación no está disponible temporalmente.");
    }

    @RequestMapping("/inventory")
    public ResponseEntity<Map<String, Object>> inventoryFallback() {
        return buildResponse("El servicio de inventario no responde. Control perimetral activado.");
    }

    @RequestMapping("/orders")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        return buildResponse("El servicio de gestión de órdenes está experimentando fallas en el backend.");
    }

    @RequestMapping("/shipments")
    public ResponseEntity<Map<String, Object>> shipmentFallback() {
        return buildResponse("El servicio de despachos y logística no se encuentra accesible.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "CIRCUIT_OPEN");
        response.put("code", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", message);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
