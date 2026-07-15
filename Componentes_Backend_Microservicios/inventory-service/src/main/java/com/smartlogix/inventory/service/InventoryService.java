package com.smartlogix.inventory.service;

import com.smartlogix.inventory.domain.InventoryItem;
import com.smartlogix.inventory.dto.*;
import com.smartlogix.inventory.exception.InventoryNotFoundException;
import com.smartlogix.inventory.exception.InventoryOperationException;
import com.smartlogix.inventory.repository.InventoryItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {
    private final InventoryItemRepository repository;

    public InventoryService(InventoryItemRepository repository) {
        this.repository = repository;
    }

    public InventoryItemResponse createItem(CreateInventoryItemRequest request) {
        String sku = normalizeSku(request.sku());
        if (repository.existsBySkuIgnoreCase(sku)) {
            throw new InventoryOperationException("El SKU ya existe: " + sku);
        }
        InventoryItem item = new InventoryItem();
        item.setSku(sku);
        item.setAvailableQuantity(request.initialQuantity());
        item.setReservedQuantity(0);
        applyProductData(item, request.productName(), request.category(), request.warehouseCode(),
                request.price(), request.description(), request.imageUrl(), request.reorderLevel(), request.active());
        return toResponse(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findAll() {
        return repository.findAllByOrderByProductNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicProductResponse> publicCatalog() {
        return repository.findByActiveTrueOrderByProductNameAsc().stream()
                .map(item -> new PublicProductResponse(item.getSku(), item.getProductName(),
                        item.getPrice(), item.getDescription(), ProductImageUrlPolicy.normalize(item.getImageUrl()), item.getAvailableQuantity() > 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse findBySku(String sku) {
        return toResponse(load(sku));
    }

    @Transactional(readOnly = true)
    public ProductSnapshotResponse snapshot(String sku, int quantity) {
        InventoryItem item = load(sku);
        return new ProductSnapshotResponse(item.getSku(), item.getProductName(), item.getPrice(), quantity,
                item.getAvailableQuantity(), item.isActive(), item.isActive() && item.getAvailableQuantity() >= quantity);
    }

    public InventoryItemResponse reserve(String sku, int quantity) {
        InventoryItem item = loadForUpdate(sku);
        requirePositive(quantity);
        if (!item.isActive() || item.getAvailableQuantity() < quantity) {
            throw new InventoryOperationException("Lo sentimos. En estos momentos este producto no tiene stock disponible.");
        }
        item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
        item.setReservedQuantity(item.getReservedQuantity() + quantity);
        return toResponse(repository.save(item));
    }

    public InventoryItemResponse release(String sku, int quantity) {
        InventoryItem item = loadForUpdate(sku);
        requirePositive(quantity);
        if (item.getReservedQuantity() < quantity) {
            throw new InventoryOperationException("No existe stock reservado suficiente para liberar.");
        }
        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        item.setAvailableQuantity(item.getAvailableQuantity() + quantity);
        return toResponse(repository.save(item));
    }

    public InventoryItemResponse dispatch(String sku, int quantity) {
        InventoryItem item = loadForUpdate(sku);
        requirePositive(quantity);
        if (item.getReservedQuantity() < quantity) {
            throw new InventoryOperationException("No existe stock reservado suficiente para despachar.");
        }
        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        return toResponse(repository.save(item));
    }

    public InventoryItemResponse updateItem(String sku, UpdateInventoryItemRequest request) {
        InventoryItem item = loadForUpdate(sku);
        applyProductData(item, request.productName(), request.category(), request.warehouseCode(), request.price(),
                request.description(), request.imageUrl(), request.reorderLevel(), request.active());
        return toResponse(repository.save(item));
    }

    public InventoryItemResponse updateStock(String sku, StockUpdateRequest request) {
        InventoryItem item = loadForUpdate(sku);
        item.setAvailableQuantity(request.availableQuantity());
        return toResponse(repository.save(item));
    }

    public void deleteItem(String sku) {
        InventoryItem item = loadForUpdate(sku);
        if (item.getReservedQuantity() > 0) {
            throw new InventoryOperationException("No puedes eliminar un producto con unidades reservadas en órdenes.");
        }
        repository.delete(item);
    }

    @Transactional(readOnly = true)
    public InventoryStatsResponse stats() {
        List<InventoryItem> items = repository.findAll();
        return new InventoryStatsResponse(
                items.size(),
                items.stream().filter(InventoryItem::isActive).count(),
                items.stream().filter(i -> i.getAvailableQuantity() == 0).count(),
                items.stream().filter(i -> i.getAvailableQuantity() > 0 && i.getAvailableQuantity() <= i.getReorderLevel()).count(),
                items.stream().mapToLong(InventoryItem::getAvailableQuantity).sum(),
                items.stream().mapToLong(InventoryItem::getReservedQuantity).sum());
    }

    private void applyProductData(InventoryItem item, String name, String category, String warehouse,
                                  java.math.BigDecimal price, String description, String imageUrl,
                                  int reorderLevel, boolean active) {
        item.setProductName(name.trim());
        item.setCategory(category.trim());
        item.setWarehouseCode(warehouse.trim().toUpperCase());
        item.setPrice(price);
        item.setDescription(description.trim());
        item.setImageUrl(ProductImageUrlPolicy.normalize(imageUrl));
        item.setReorderLevel(reorderLevel);
        item.setActive(active);
    }

    private InventoryItem load(String sku) {
        return repository.findBySkuIgnoreCase(normalizeSku(sku))
                .orElseThrow(() -> new InventoryNotFoundException("No existe el producto con SKU: " + sku));
    }

    private InventoryItem loadForUpdate(String sku) {
        return repository.findBySkuForUpdate(normalizeSku(sku))
                .orElseThrow(() -> new InventoryNotFoundException("No existe el producto con SKU: " + sku));
    }

    private String normalizeSku(String sku) { return sku.trim().toUpperCase(); }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new InventoryOperationException("La cantidad debe ser mayor que cero.");
        }
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(item.getSku(), item.getProductName(), item.getCategory(),
                item.getWarehouseCode(), item.getPrice(), item.getDescription(), ProductImageUrlPolicy.normalize(item.getImageUrl()),
                item.isActive(), item.getAvailableQuantity(), item.getReservedQuantity(),
                item.getReorderLevel(), item.getUpdatedAt());
    }
}
