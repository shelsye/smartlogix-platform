package com.smartlogix.order.config;

import com.smartlogix.order.domain.DiscountCoupon;
import com.smartlogix.order.repository.DiscountCouponRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CouponSeedConfig {
    @Bean
    CommandLineRunner ensureWelcomeCoupon(
            DiscountCouponRepository repository,
            @Value("${smartlogix.welcome-coupon-code}") String code,
            @Value("${smartlogix.welcome-coupon-percent}") int percentage) {
        return args -> {
            if (repository.findByCodeIgnoreCase(code).isPresent()) return;
            DiscountCoupon coupon = new DiscountCoupon();
            coupon.setCode(code.trim().toUpperCase());
            coupon.setDiscountPercentage(percentage);
            coupon.setActive(true);
            coupon.setSingleUse(true);
            coupon.setDescription("Cupón de bienvenida para nuevos clientes");
            repository.save(coupon);
        };
    }
}
