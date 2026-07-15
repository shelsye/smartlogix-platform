package com.smartlogix.order.repository;

import com.smartlogix.order.domain.PurchaseOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByOrderNumberIgnoreCase(String orderNumber);
    List<PurchaseOrder> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId);
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
}
