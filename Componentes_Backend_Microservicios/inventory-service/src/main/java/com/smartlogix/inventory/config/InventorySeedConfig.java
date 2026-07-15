package com.smartlogix.inventory.config;

import com.smartlogix.inventory.domain.InventoryItem;
import com.smartlogix.inventory.repository.InventoryItemRepository;
import com.smartlogix.inventory.service.ProductImageUrlPolicy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conserva la estructura original y no crea ni elimina productos.
 * Únicamente corrige imágenes vacías o rutas locales incompatibles con Docker.
 */
@Configuration
public class InventorySeedConfig {
    @Bean
    CommandLineRunner repairExistingProductImages(InventoryItemRepository repository) {
        return args -> {
            for (InventoryItem item : repository.findAll()) {
                String normalized = ProductImageUrlPolicy.normalize(item.getImageUrl());
                if (!normalized.equals(item.getImageUrl())) {
                    item.setImageUrl(normalized);
                    repository.save(item);
                }
            }
        };
    }
}
