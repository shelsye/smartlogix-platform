package com.smartlogix.inventory.controller;

import com.smartlogix.inventory.dto.*;
import com.smartlogix.inventory.exception.InventoryOperationException;
import com.smartlogix.inventory.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {
    private final InventoryService service;
    private final String internalApiKey;

    public InventoryController(InventoryService service,
                               @Value("${smartlogix.internal-api-key}") String internalApiKey) {
        this.service = service;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/catalog")
    public List<PublicProductResponse> catalog() { return service.publicCatalog(); }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemResponse create(@Valid @RequestBody CreateInventoryItemRequest request) {
        return service.createItem(request);
    }

    @GetMapping("/items")
    public List<InventoryItemResponse> list() { return service.findAll(); }

    @GetMapping("/items/{sku}")
    public InventoryItemResponse find(@PathVariable String sku) { return service.findBySku(sku); }

    @PutMapping("/items/{sku}")
    public InventoryItemResponse update(@PathVariable String sku,
                                        @Valid @RequestBody UpdateInventoryItemRequest request) {
        return service.updateItem(sku, request);
    }

    @PatchMapping("/items/{sku}/stock")
    public InventoryItemResponse stock(@PathVariable String sku,
                                       @Valid @RequestBody StockUpdateRequest request) {
        return service.updateStock(sku, request);
    }

    @DeleteMapping("/items/{sku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String sku) { service.deleteItem(sku); }

    @GetMapping("/statistics")
    public InventoryStatsResponse statistics() { return service.stats(); }

    @GetMapping("/internal/items/{sku}/snapshot")
    public ProductSnapshotResponse snapshot(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable String sku,
            @RequestParam @Min(1) int quantity) {
        verifyInternalKey(key);
        return service.snapshot(sku, quantity);
    }

    @PostMapping("/internal/items/{sku}/reserve")
    public InventoryItemResponse reserve(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable String sku,
            @RequestParam @Min(1) int quantity) {
        verifyInternalKey(key);
        return service.reserve(sku, quantity);
    }

    @PostMapping("/internal/items/{sku}/release")
    public InventoryItemResponse release(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable String sku,
            @RequestParam @Min(1) int quantity) {
        verifyInternalKey(key);
        return service.release(sku, quantity);
    }

    @PostMapping("/internal/items/{sku}/dispatch")
    public InventoryItemResponse dispatch(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable String sku,
            @RequestParam @Min(1) int quantity) {
        verifyInternalKey(key);
        return service.dispatch(sku, quantity);
    }

    private void verifyInternalKey(String key) {
        if (!internalApiKey.equals(key)) {
            throw new InventoryOperationException("Acceso interno no autorizado.");
        }
    }
}
