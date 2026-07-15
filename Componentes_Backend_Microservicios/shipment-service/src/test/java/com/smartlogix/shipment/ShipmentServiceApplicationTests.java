package com.smartlogix.shipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShipmentServiceApplicationTests {
    @Test
    void configuracionContieneLasDieciseisRegionesDeChile() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/regions.csv"), StandardCharsets.UTF_8))) {
            List<String> regions = reader.lines().skip(1).map(line -> line.split(";", -1)[0]).toList();
            assertEquals(16, regions.size());
            assertTrue(regions.contains("Arica y Parinacota"));
            assertTrue(regions.contains("Metropolitana de Santiago"));
            assertTrue(regions.contains("Magallanes"));
        }
    }
}
