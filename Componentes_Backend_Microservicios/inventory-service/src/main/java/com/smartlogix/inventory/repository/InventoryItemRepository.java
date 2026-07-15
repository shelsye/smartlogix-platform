package com.smartlogix.inventory.repository;

import com.smartlogix.inventory.domain.InventoryItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCase(String sku);
    List<InventoryItem> findAllByOrderByProductNameAsc();
    List<InventoryItem> findByActiveTrueOrderByProductNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where upper(i.sku) = upper(:sku)")
    Optional<InventoryItem> findBySkuForUpdate(@Param("sku") String sku);
}
