package com.smartlogix.shipment.repository;

import com.smartlogix.shipment.domain.Shipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingCodeIgnoreCase(String trackingCode);
    Optional<Shipment> findByOrderNumberIgnoreCase(String orderNumber);
}
