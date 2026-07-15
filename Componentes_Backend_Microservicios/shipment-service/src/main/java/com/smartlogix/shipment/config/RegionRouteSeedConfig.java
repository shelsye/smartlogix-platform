package com.smartlogix.shipment.config;

import com.smartlogix.shipment.domain.RegionRouteConfig;
import com.smartlogix.shipment.repository.RegionRouteConfigRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegionRouteSeedConfig {
    @Bean
    CommandLineRunner seedRegions(RegionRouteConfigRepository repository) {
        return args -> {
            if (repository.count() > 0) return;
            ClassPathResource resource = new ClassPathResource("regions.csv");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                reader.lines().skip(1).filter(line -> !line.isBlank()).forEach(line -> {
                    String[] p = line.split(";", -1);
                    RegionRouteConfig config = new RegionRouteConfig();
                    config.setRegionName(p[0]);
                    config.setDistanceKm(Integer.parseInt(p[1]));
                    config.setRemoteFactor(Double.parseDouble(p[2]));
                    config.setEconomyCarrier(p[3]);
                    config.setBalancedCarrier(p[4]);
                    config.setExpressCarrier(p[5]);
                    repository.save(config);
                });
            }
        };
    }
}
