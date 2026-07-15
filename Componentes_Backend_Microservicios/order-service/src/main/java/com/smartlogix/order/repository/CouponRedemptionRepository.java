package com.smartlogix.order.repository;

import com.smartlogix.order.domain.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    boolean existsByCustomerUserIdAndCouponCodeIgnoreCase(Long customerUserId, String couponCode);
}
