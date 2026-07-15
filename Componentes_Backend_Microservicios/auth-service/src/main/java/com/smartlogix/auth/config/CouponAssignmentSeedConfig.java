package com.smartlogix.auth.config;

import com.smartlogix.auth.domain.Role;
import com.smartlogix.auth.domain.UserCoupon;
import com.smartlogix.auth.repository.UserCouponRepository;
import com.smartlogix.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class CouponAssignmentSeedConfig {
    @Bean
    @Order(2)
    CommandLineRunner ensureExistingClientsHaveCoupon(
            UserRepository users,
            UserCouponRepository coupons,
            @Value("${smartlogix.welcome-coupon-code:BIENVENIDA10}") String code,
            @Value("${smartlogix.welcome-coupon-percent:10}") int percentage) {
        return args -> users.findAll().stream()
                .filter(user -> user.getRole() == Role.ROLE_USER)
                .filter(user -> !coupons.existsByUserIdAndCodeIgnoreCase(user.getId(), code))
                .forEach(user -> {
                    UserCoupon coupon = new UserCoupon();
                    coupon.setUserId(user.getId());
                    coupon.setCode(code.trim().toUpperCase());
                    coupon.setDiscountPercentage(percentage);
                    coupon.setUsed(false);
                    coupons.save(coupon);
                });
    }
}
