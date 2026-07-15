package com.smartlogix.auth.repository;

import com.smartlogix.auth.domain.UserCoupon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    Optional<UserCoupon> findByUserIdAndCodeIgnoreCase(Long userId, String code);

    boolean existsByUserIdAndCodeIgnoreCase(Long userId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from UserCoupon c where c.userId = :userId and upper(c.code) = upper(:code)")
    Optional<UserCoupon> findForUpdate(@Param("userId") Long userId, @Param("code") String code);
}
