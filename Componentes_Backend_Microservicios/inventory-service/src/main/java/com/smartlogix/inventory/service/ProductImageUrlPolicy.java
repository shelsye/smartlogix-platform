package com.smartlogix.inventory.service;

import java.util.Locale;

public final class ProductImageUrlPolicy {
    public static final String DEFAULT_IMAGE_URL = "/product-placeholder.svg";

    private ProductImageUrlPolicy() {
    }

    public static String normalize(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return DEFAULT_IMAGE_URL;
        }
        String value = imageUrl.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        boolean external = lower.startsWith("https://") || lower.startsWith("http://");
        boolean bundledDefault = value.equals(DEFAULT_IMAGE_URL);
        if (bundledDefault) {
            return value;
        }
        if (!external || lower.startsWith("http://localhost") || lower.startsWith("https://localhost")
                || lower.startsWith("http://127.0.0.1") || lower.startsWith("https://127.0.0.1")) {
            return DEFAULT_IMAGE_URL;
        }
        return value;
    }
}
