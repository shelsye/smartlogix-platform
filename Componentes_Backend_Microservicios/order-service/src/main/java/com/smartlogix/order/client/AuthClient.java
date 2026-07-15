package com.smartlogix.order.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthClient {
    private final RestTemplate restTemplate;
    private final String internalApiKey;

    public AuthClient(RestTemplate restTemplate,
                      @Value("${smartlogix.internal-api-key}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.internalApiKey = internalApiKey;
    }

    public UserCouponResponse couponStatus(Long userId, String code) {
        return exchange(base(userId, code), HttpMethod.GET, UserCouponResponse.class);
    }

    public UserCouponResponse consume(Long userId, String code, String orderNumber) {
        return exchange(base(userId, code) + "/consume?orderNumber=" + encode(orderNumber),
                HttpMethod.POST, UserCouponResponse.class);
    }

    public void release(Long userId, String code, String orderNumber) {
        exchange(base(userId, code) + "/release?orderNumber=" + encode(orderNumber),
                HttpMethod.POST, UserCouponResponse.class);
    }

    private String base(Long userId, String code) {
        return "http://auth-service/api/auth/internal/users/" + userId + "/coupons/" + encode(code);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private <T> T exchange(String url, HttpMethod method, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            ResponseEntity<T> response = restTemplate.exchange(
                    url, method, new HttpEntity<Void>(headers), responseType);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            throw new IllegalStateException(body == null || body.isBlank()
                    ? "El servicio de usuarios rechazó el cupón." : body, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "No fue posible conectar con el servicio de usuarios: " + ex.getMessage(), ex);
        }
    }
}
