package com.smartlogix.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class OrderServiceApplicationTests {
    @Test
    void bienvenida10AplicaDiezPorCientoReal() {
        BigDecimal subtotal = new BigDecimal("100000");
        int porcentaje = 10;
        BigDecimal descuento = subtotal.multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("10000"), descuento);
        assertEquals(new BigDecimal("90000"), subtotal.subtract(descuento));
    }
}
