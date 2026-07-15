package com.smartlogix.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryClient {
    private final RestTemplate restTemplate;
    private final String internalApiKey;

    public InventoryClient(RestTemplate restTemplate,
                           @Value("${smartlogix.internal-api-key}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.internalApiKey = internalApiKey;
    }

    public ProductSnapshotResponse snapshot(String sku, int quantity) {
        try {
            ResponseEntity<ProductSnapshotResponse> response = restTemplate.exchange(
                    "http://inventory-service/api/inventory/internal/items/{sku}/snapshot?quantity={quantity}",
                    HttpMethod.GET, entity(), ProductSnapshotResponse.class, sku, quantity);
            ProductSnapshotResponse body = response.getBody();
            if (body == null) throw new InventoryClientException("Inventario respondió sin datos.", null);
            return body;
        } catch (HttpStatusCodeException ex) {
            throw new InventoryClientException(readMessage(ex, "No se pudo consultar el inventario."), ex);
        } catch (RestClientException ex) {
            throw new InventoryClientException("No fue posible conectar con el inventario: " + ex.getMessage(), ex);
        }
    }

    public void reserve(String sku, int quantity) { post(sku, quantity, "reserve"); }
    public void release(String sku, int quantity) { post(sku, quantity, "release"); }
    public void dispatch(String sku, int quantity) { post(sku, quantity, "dispatch"); }

    private void post(String sku, int quantity, String action) {
        try {
            restTemplate.exchange(
                    "http://inventory-service/api/inventory/internal/items/{sku}/" + action + "?quantity={quantity}",
                    HttpMethod.POST, entity(), Void.class, sku, quantity);
        } catch (HttpStatusCodeException ex) {
            throw new InventoryClientException(readMessage(ex, "El inventario rechazó la operación."), ex);
        } catch (RestClientException ex) {
            throw new InventoryClientException("No fue posible conectar con el inventario: " + ex.getMessage(), ex);
        }
    }

    private HttpEntity<Void> entity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        return new HttpEntity<>(headers);
    }

    private String readMessage(HttpStatusCodeException ex, String fallback) {
        String body = ex.getResponseBodyAsString();
        return body == null || body.isBlank() ? fallback : body;
    }
}
