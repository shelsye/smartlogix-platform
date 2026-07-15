package com.smartlogix.pricefinder.service;

import com.smartlogix.pricefinder.dto.PriceOptionResponse;
import java.util.List;

public interface ExternalPriceClient {
    List<PriceOptionResponse> search(String query, int limit);
    String sourceName();
    boolean enabled();
}
