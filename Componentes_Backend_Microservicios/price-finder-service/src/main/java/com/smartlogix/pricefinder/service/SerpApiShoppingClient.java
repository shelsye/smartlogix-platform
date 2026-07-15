package com.smartlogix.pricefinder.service;

import com.smartlogix.pricefinder.dto.PriceOptionResponse;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SerpApiShoppingClient implements ExternalPriceClient {
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String country;
    private final String language;
    private final String googleDomain;
    private final String currency;

    public SerpApiShoppingClient(RestTemplateBuilder builder,
                                @Value("${smartlogix.price-finder.serpapi-key:}") String apiKey,
                                @Value("${smartlogix.price-finder.country:cl}") String country,
                                @Value("${smartlogix.price-finder.language:es}") String language,
                                @Value("${smartlogix.price-finder.google-domain:google.cl}") String googleDomain,
                                @Value("${smartlogix.price-finder.currency:CLP}") String currency,
                                @Value("${smartlogix.price-finder.request-timeout-seconds:12}") long timeoutSeconds) {
        this.restTemplate = builder.setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.country = country;
        this.language = language;
        this.googleDomain = googleDomain;
        this.currency = currency;
    }

    @Override
    public List<PriceOptionResponse> search(String query, int limit) {
        if (!enabled()) return List.of();
        String url = "https://serpapi.com/search.json?engine=google_shopping"
                + "&q=" + encode(query)
                + "&gl=" + encode(country)
                + "&hl=" + encode(language)
                + "&google_domain=" + encode(googleDomain)
                + "&api_key=" + encode(apiKey);
        Map<?, ?> response = restTemplate.getForObject(URI.create(url), Map.class);
        List<PriceOptionResponse> out = new ArrayList<>();
        addShoppingResults(out, response == null ? null : response.get("shopping_results"), limit);
        addShoppingResults(out, response == null ? null : response.get("inline_shopping_results"), limit);
        return out.stream().limit(limit).toList();
    }

    private void addShoppingResults(List<PriceOptionResponse> out, Object object, int limit) {
        if (!(object instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            String title = text(map.get("title"));
            String store = firstText(map, "source", "seller", "merchant", "store");
            BigDecimal price = number(map.get("extracted_price"));
            if (price == null) price = parsePrice(text(map.get("price")));
            if (title.isBlank() || store.isBlank() || price == null || price.signum() <= 0) continue;
            String link = firstText(map, "link", "product_link");
            String image = firstText(map, "thumbnail", "thumbnail_image");
            String delivery = firstText(map, "delivery", "shipping");
            out.add(new PriceOptionResponse(store, store, title, price, currency, "Nuevo", link, image, delivery, sourceName()));
            if (out.size() >= limit) return;
        }
    }

    @Override
    public String sourceName() { return "GOOGLE_SHOPPING_SERPAPI"; }

    @Override
    public boolean enabled() { return !apiKey.isBlank(); }

    private String firstText(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            String value = text(map.get(key));
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }

    private BigDecimal number(Object value) {
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).setScale(0, java.math.RoundingMode.HALF_UP);
        return parsePrice(text(value));
    }

    private BigDecimal parsePrice(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.toUpperCase(Locale.ROOT).replace("CLP", "").replace("$", "").replace(".", "").replace(",", ".").replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) return null;
        try { return new BigDecimal(normalized).setScale(0, java.math.RoundingMode.HALF_UP); }
        catch (RuntimeException ex) { return null; }
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
