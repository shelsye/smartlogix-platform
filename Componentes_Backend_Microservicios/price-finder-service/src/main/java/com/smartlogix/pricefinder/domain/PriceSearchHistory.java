package com.smartlogix.pricefinder.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "price_search_history")
public class PriceSearchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", length = 120)
    private String userEmail;

    @Column(nullable = false, length = 160)
    private String query;

    @Column(name = "best_store", length = 120)
    private String bestStore;

    @Column(name = "best_product", length = 500)
    private String bestProduct;

    @Column(name = "best_price", precision = 14, scale = 2)
    private BigDecimal bestPrice;

    @Column(length = 10)
    private String currency;

    @Column(name = "results_count", nullable = false)
    private int resultsCount;

    @Column(name = "source_mode", length = 80)
    private String sourceMode;

    @Column(name = "searched_at", nullable = false, updatable = false)
    private OffsetDateTime searchedAt;

    @PrePersist
    void prePersist() { searchedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getBestStore() { return bestStore; }
    public void setBestStore(String bestStore) { this.bestStore = bestStore; }
    public String getBestProduct() { return bestProduct; }
    public void setBestProduct(String bestProduct) { this.bestProduct = bestProduct; }
    public BigDecimal getBestPrice() { return bestPrice; }
    public void setBestPrice(BigDecimal bestPrice) { this.bestPrice = bestPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getResultsCount() { return resultsCount; }
    public void setResultsCount(int resultsCount) { this.resultsCount = resultsCount; }
    public String getSourceMode() { return sourceMode; }
    public void setSourceMode(String sourceMode) { this.sourceMode = sourceMode; }
    public OffsetDateTime getSearchedAt() { return searchedAt; }
}
