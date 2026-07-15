package com.smartlogix.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ShipmentClient {
    private final RestTemplate restTemplate;
    private final String internalApiKey;

    public ShipmentClient(RestTemplate restTemplate,
                          @Value("${smartlogix.internal-api-key}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.internalApiKey = internalApiKey;
    }

    public ShipmentResponse create(ShipmentRequest request) {
        return exchange("http://shipment-service/api/shipments/internal", HttpMethod.POST, request, ShipmentResponse.class);
    }

    public ShipmentResponse createFromSelection(ShipmentSelectionRequest request) {
        return exchange("http://shipment-service/api/shipments/internal/from-selection",
                HttpMethod.POST, request, ShipmentResponse.class);
    }

    public ShipmentResponse update(String orderNumber, UpdateShipmentRequest request) {
        return exchange("http://shipment-service/api/shipments/internal/order/" + orderNumber,
                HttpMethod.PUT, request, ShipmentResponse.class);
    }

    public void updateStatus(String orderNumber, String status) {
        exchange("http://shipment-service/api/shipments/internal/order/" + orderNumber + "/status?value=" + status,
                HttpMethod.POST, null, ShipmentResponse.class);
    }

    public void cancel(String orderNumber) {
        exchange("http://shipment-service/api/shipments/internal/order/" + orderNumber + "/cancel",
                HttpMethod.POST, null, Void.class);
    }

    public void rollback(String orderNumber) {
        exchange("http://shipment-service/api/shipments/internal/order/" + orderNumber + "/rollback",
                HttpMethod.POST, null, Void.class);
    }

    private <T> T exchange(String url, HttpMethod method, Object body, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Api-Key", internalApiKey);
            ResponseEntity<T> response = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), responseType);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            String response = ex.getResponseBodyAsString();
            throw new IllegalStateException(response == null || response.isBlank() ? "El servicio de envíos rechazó la operación." : response, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("No fue posible conectar con el servicio de envíos: " + ex.getMessage(), ex);
        }
    }
}
