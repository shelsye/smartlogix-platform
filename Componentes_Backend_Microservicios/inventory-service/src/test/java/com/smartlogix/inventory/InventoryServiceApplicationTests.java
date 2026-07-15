package com.smartlogix.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smartlogix.inventory.domain.InventoryItem;
import org.junit.jupiter.api.Test;

class InventoryServiceApplicationTests {
    @Test
    void reservaDescuentaDisponibleYAumentaReservado() {
        InventoryItem item = new InventoryItem();
        item.setAvailableQuantity(8);
        item.setReservedQuantity(0);

        int cantidad = 3;
        item.setAvailableQuantity(item.getAvailableQuantity() - cantidad);
        item.setReservedQuantity(item.getReservedQuantity() + cantidad);

        assertEquals(5, item.getAvailableQuantity());
        assertEquals(3, item.getReservedQuantity());
    }
}
