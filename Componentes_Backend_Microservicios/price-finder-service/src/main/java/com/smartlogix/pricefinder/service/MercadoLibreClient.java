package com.smartlogix.pricefinder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogix.pricefinder.dto.PriceOptionResponse;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MercadoLibreClient implements ExternalPriceClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String siteId;
    private final String currency;
    private final long timeoutSeconds;

    public MercadoLibreClient(ObjectMapper objectMapper,
                              @Value("${smartlogix.price-finder.mercado-libre-site:MLC}") String siteId,
                              @Value("${smartlogix.price-finder.currency:CLP}") String currency,
                              @Value("${smartlogix.price-finder.request-timeout-seconds:18}") long timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.siteId = siteId == null || siteId.isBlank() ? "MLC" : siteId.trim();
        this.currency = currency == null || currency.isBlank() ? "CLP" : currency.trim();
        this.timeoutSeconds = Math.max(8, timeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public List<PriceOptionResponse> search(String query, int limit) {
        int safeLimit = Math.max(10, Math.min(limit, 50));
        String url = "https://api.mercadolibre.com/sites/" + encode(siteId)
                + "/search?q=" + encode(query)
                + "&limit=" + safeLimit;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 SmartLogix-PriceFinder/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();
            Map<?, ?> body = objectMapper.readValue(response.body(), Map.class);
            if (!(body.get("results") instanceof List<?> results)) return List.of();

            List<PriceOptionResponse> out = new ArrayList<>();
            for (Object item : results) {
                if (!(item instanceof Map<?, ?> map)) continue;
                String title = text(map.get("title"));
                BigDecimal price = number(map.get("price"));
                if (title.isBlank() || price == null || price.signum() <= 0) continue;

                String store = bestStoreName(map);
                String seller = sellerName(map.get("seller"));
                String condition = text(map.get("condition"));
                if (condition.equalsIgnoreCase("new")) condition = "Nuevo";
                if (condition.equalsIgnoreCase("used")) condition = "Usado";
                if (condition.isBlank()) condition = "Nuevo";
                String link = text(map.get("permalink"));
                String image = text(map.get("thumbnail"));
                String delivery = deliveryText(map.get("shipping"));

                out.add(new PriceOptionResponse(
                        store,
                        seller.isBlank() ? "Mercado Libre Chile" : seller,
                        title,
                        price,
                        currency,
                        condition,
                        link,
                        image,
                        delivery,
                        sourceName()
                ));
                if (out.size() >= safeLimit) break;
            }
            return out;
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Override
    public String sourceName() { return "MERCADO_LIBRE_API"; }

    @Override
    public boolean enabled() { return true; }

    private String bestStoreName(Map<?, ?> map) {
        String official = text(map.get("official_store_name"));
        if (!official.isBlank()) return official;
        String seller = sellerName(map.get("seller"));
        if (!seller.isBlank()) return seller;
        String catalog = text(map.get("domain_id"));
        if (!catalog.isBlank()) return "Tienda " + catalog.replace("MLC-", "").replace('_', ' ');
        return "Tienda Mercado Libre Chile";
    }

    private String sellerName(Object sellerObject) {
        if (sellerObject instanceof Map<?, ?> seller) {
            String nick = text(seller.get("nickname"));
            if (!nick.isBlank()) return nick;
            String id = text(seller.get("id"));
            if (!id.isBlank()) return "Vendedor " + id;
        }
        return "";
    }

    private String deliveryText(Object shippingObject) {
        if (shippingObject instanceof Map<?, ?> shipping) {
            Object free = shipping.get("free_shipping");
            if (Boolean.TRUE.equals(free)) return "Envío disponible";
        }
        return "Según tienda";
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }

    private BigDecimal number(Object value) {
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).setScale(0, java.math.RoundingMode.HALF_UP);
        String raw = text(value).replace(".", "").replace(",", ".").replaceAll("[^0-9.]", "");
        if (raw.isBlank()) return null;
        try { return new BigDecimal(raw).setScale(0, java.math.RoundingMode.HALF_UP); }
        catch (RuntimeException ex) { return null; }
    }

    private String encode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
}
