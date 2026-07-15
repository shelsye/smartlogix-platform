package com.smartlogix.pricefinder.controller;

import com.smartlogix.pricefinder.dto.PriceSearchHistoryResponse;
import com.smartlogix.pricefinder.dto.PriceSearchResponse;
import com.smartlogix.pricefinder.service.PriceFinderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/price-finder")
public class PriceFinderController {
    private final PriceFinderService service;

    public PriceFinderController(PriceFinderService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public PriceSearchResponse search(@RequestParam String query,
                                      @RequestParam(defaultValue = "20") int limit,
                                      @RequestHeader(value = "X-Auth-User", required = false) String userEmail) {
        return service.search(query, limit, userEmail);
    }

    @GetMapping("/history")
    public List<PriceSearchHistoryResponse> history() {
        return service.history();
    }
}
