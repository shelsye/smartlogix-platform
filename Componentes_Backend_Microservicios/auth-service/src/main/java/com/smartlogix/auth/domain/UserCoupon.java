package com.smartlogix.auth.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_coupons",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coupon_code"}))
public class UserCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_code", nullable = false, length = 30)
    private String code;

    @Column(nullable = false)
    private int discountPercentage;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime issuedAt;

    private OffsetDateTime usedAt;

    @Column(length = 50)
    private String orderNumber;

    @PrePersist
    void onCreate() { issuedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public int getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(int discountPercentage) { this.discountPercentage = discountPercentage; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
}
