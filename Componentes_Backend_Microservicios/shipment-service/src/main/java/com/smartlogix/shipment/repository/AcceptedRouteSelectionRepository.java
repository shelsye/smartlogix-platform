package com.smartlogix.shipment.repository;

import com.smartlogix.shipment.domain.AcceptedRouteSelection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcceptedRouteSelectionRepository extends JpaRepository<AcceptedRouteSelection, Long> {
    Optional<AcceptedRouteSelection> findFirstByUserIdAndOrderNumberIsNullOrderByUpdatedAtDesc(Long userId);
    Optional<AcceptedRouteSelection> findBySelectionIdAndUserId(String selectionId, Long userId);
    Optional<AcceptedRouteSelection> findByOrderNumberIgnoreCase(String orderNumber);
}
