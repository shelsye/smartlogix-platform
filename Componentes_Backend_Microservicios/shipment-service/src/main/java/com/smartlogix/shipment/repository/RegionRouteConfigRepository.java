package com.smartlogix.shipment.repository;

import com.smartlogix.shipment.domain.RegionRouteConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRouteConfigRepository extends JpaRepository<RegionRouteConfig, Long> {
    Optional<RegionRouteConfig> findByRegionNameIgnoreCase(String regionName);
    List<RegionRouteConfig> findAllByOrderByRegionNameAsc();
}
