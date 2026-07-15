package com.smartlogix.shipment.controller;

import com.smartlogix.shipment.domain.ShipmentStatus;
import com.smartlogix.shipment.dto.*;
import com.smartlogix.shipment.security.AuthenticatedUser;
import com.smartlogix.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    private final ShipmentService service;
    private final String internalApiKey;

    public ShipmentController(ShipmentService service,
                              @Value("${smartlogix.internal-api-key}") String internalApiKey) {
        this.service = service;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/regions")
    public List<String> regions() { return service.regions(); }

    @PostMapping("/recommendations")
    public List<RouteOptionResponse> recommendations(@Valid @RequestBody RouteRecommendationRequest request) {
        return service.recommendRoutes(request);
    }

    @PostMapping("/selections")
    public ResponseEntity<AcceptedRouteResponse> acceptSelection(
            Authentication authentication,
            @Valid @RequestBody AcceptRouteRequest request) {
        return ResponseEntity.ok(service.acceptRoute(current(authentication).userId(), request));
    }

    @GetMapping("/selections/current")
    public ResponseEntity<AcceptedRouteResponse> currentSelection(Authentication authentication) {
        return service.currentSelection(current(authentication).userId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/selections/current")
    public ResponseEntity<AcceptedRouteResponse> clearSelection(Authentication authentication) {
        return service.clearCurrentSelection(current(authentication).userId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public List<ShipmentResponse> list() { return service.getShipments(); }

    @GetMapping("/{trackingCode}")
    public ShipmentResponse find(@PathVariable String trackingCode) {
        return service.getByTrackingCode(trackingCode);
    }

    @PatchMapping("/{trackingCode}/status")
    public ShipmentResponse status(@PathVariable String trackingCode,
                                   @RequestParam ShipmentStatus value) {
        return service.updateStatus(trackingCode, value);
    }

    @PostMapping("/{trackingCode}/adapt-carrier")
    public ShipmentResponse adapt(@PathVariable String trackingCode,
                                  @Valid @RequestBody UpdateShipmentPlanRequest request) {
        return service.adaptCarrier(trackingCode, request);
    }

    @DeleteMapping("/{trackingCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String trackingCode) {
        service.deleteShipment(trackingCode);
    }

    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createInternal(
            @RequestHeader("X-Internal-Api-Key") String key,
            @Valid @RequestBody CreateShipmentRequest request) {
        verifyKey(key);
        return service.createShipment(request);
    }

    @PostMapping("/internal/from-selection")
    public ShipmentResponse createFromSelectionInternal(
            @RequestHeader("X-Internal-Api-Key") String key,
            @Valid @RequestBody CreateShipmentFromSelectionRequest request) {
        verifyKey(key);
        return service.createShipmentFromSelection(request);
    }

    @PutMapping("/internal/order/{orderNumber}")
    public ShipmentResponse updateInternal(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateShipmentPlanRequest request) {
        verifyKey(key);
        return service.updateByOrderNumber(orderNumber, request);
    }

    @PostMapping("/internal/order/{orderNumber}/status")
    public ShipmentResponse statusInternal(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable String orderNumber,
            @RequestParam ShipmentStatus value) {
        verifyKey(key);
        return service.updateStatusByOrderNumber(orderNumber, value);
    }

    @PostMapping("/internal/order/{orderNumber}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInternal(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable String orderNumber) {
        verifyKey(key);
        service.cancelByOrderNumber(orderNumber);
    }

    @PostMapping("/internal/order/{orderNumber}/rollback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rollbackInternal(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable String orderNumber) {
        verifyKey(key);
        service.rollbackByOrderNumber(orderNumber);
    }

    private AuthenticatedUser current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No fue posible identificar al usuario autenticado.");
        }
        return user;
    }

    private void verifyKey(String key) {
        if (!internalApiKey.equals(key)) {
            throw new IllegalArgumentException("Acceso interno no autorizado.");
        }
    }
}
