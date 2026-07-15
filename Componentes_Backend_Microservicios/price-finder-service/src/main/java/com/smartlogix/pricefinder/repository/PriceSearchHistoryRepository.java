package com.smartlogix.pricefinder.repository;

import com.smartlogix.pricefinder.domain.PriceSearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSearchHistoryRepository extends JpaRepository<PriceSearchHistory, Long> {}
