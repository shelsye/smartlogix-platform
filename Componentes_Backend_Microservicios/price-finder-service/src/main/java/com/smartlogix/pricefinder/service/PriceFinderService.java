package com.smartlogix.pricefinder.service;

import com.smartlogix.pricefinder.domain.PriceSearchHistory;
import com.smartlogix.pricefinder.dto.PriceOptionResponse;
import com.smartlogix.pricefinder.dto.PriceSearchHistoryResponse;
import com.smartlogix.pricefinder.dto.PriceSearchResponse;
import com.smartlogix.pricefinder.exception.PriceFinderException;
import com.smartlogix.pricefinder.repository.PriceSearchHistoryRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PriceFinderService {
    private final List<ExternalPriceClient> clients;
    private final PriceSearchHistoryRepository historyRepository;

    public PriceFinderService(List<ExternalPriceClient> clients, PriceSearchHistoryRepository historyRepository) {
        this.clients = clients;
        this.historyRepository = historyRepository;
    }

    public PriceSearchResponse search(String query, int limit, String userEmail) {
        String cleanQuery = normalizeQuery(query);
        int safeLimit = Math.max(5, Math.min(limit <= 0 ? 20 : limit, 50));
        List<PriceOptionResponse> raw = new ArrayList<>();
        List<String> usedSources = new ArrayList<>();

        for (ExternalPriceClient client : clients) {
            if (!client.enabled()) continue;
            try {
                List<PriceOptionResponse> partial = client.search(cleanQuery, safeLimit);
                if (!partial.isEmpty()) {
                    usedSources.add(client.sourceName());
                    raw.addAll(partial);
                }
            } catch (RuntimeException ex) {
                // Si una fuente externa falla, no se cae todo el comparador.
            }
        }

        List<PriceOptionResponse> sorted = deduplicate(raw).stream()
                .filter(option -> option.price() != null && option.price().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(PriceOptionResponse::price))
                .limit(safeLimit)
                .toList();

        if (sorted.isEmpty()) {
            throw new PriceFinderException("No se encontraron precios para: " + cleanQuery + ". Prueba con otro producto.");
        }

        PriceOptionResponse best = sorted.get(0);
        String sourceMode = usedSources.contains("GOOGLE_SHOPPING_SERPAPI")
                ? "REAL_MULTITIENDA_GOOGLE_SHOPPING"
                : "REAL_MARKETPLACE_MERCADO_LIBRE";
        String message = usedSources.contains("GOOGLE_SHOPPING_SERPAPI")
                ? "Resultados encontrados en tiendas externas."
                : "Resultados encontrados en tiendas reales disponibles.";

        saveHistory(userEmail, cleanQuery, best, sorted.size(), sourceMode);
        return new PriceSearchResponse(cleanQuery, sorted.size(), false, "SIMBOLICO", sourceMode, message, best, sorted);
    }

    @Transactional(readOnly = true)
    public List<PriceSearchHistoryResponse> history() {
        return historyRepository.findAll().stream()
                .sorted(Comparator.comparing(PriceSearchHistory::getSearchedAt).reversed())
                .limit(50)
                .map(h -> new PriceSearchHistoryResponse(h.getId(), h.getQuery(), h.getBestStore(), h.getBestProduct(),
                        h.getBestPrice(), h.getCurrency(), h.getResultsCount(), h.getSourceMode(), h.getSearchedAt()))
                .toList();
    }

    private void saveHistory(String userEmail, String query, PriceOptionResponse best, int count, String sourceMode) {
        PriceSearchHistory history = new PriceSearchHistory();
        history.setUserEmail(userEmail == null || userEmail.isBlank() ? "anonimo" : userEmail.toLowerCase(Locale.ROOT));
        history.setQuery(query);
        history.setBestStore(best.store());
        history.setBestProduct(best.title());
        history.setBestPrice(best.price());
        history.setCurrency(best.currency());
        history.setResultsCount(count);
        history.setSourceMode(sourceMode);
        historyRepository.save(history);
    }

    private List<PriceOptionResponse> deduplicate(List<PriceOptionResponse> input) {
        Map<String, PriceOptionResponse> unique = new LinkedHashMap<>();
        for (PriceOptionResponse option : input) {
            String key = (option.store() + "|" + option.title() + "|" + option.price()).toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, option);
        }
        return new ArrayList<>(unique.values());
    }

    private String normalizeQuery(String query) {
        String clean = query == null ? "" : query.trim().replaceAll("\\s+", " ");
        if (clean.length() < 2) throw new PriceFinderException("Debes ingresar un producto para buscar.");
        if (clean.length() > 120) throw new PriceFinderException("La búsqueda es demasiado larga.");
        return clean;
    }
}
