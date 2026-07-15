package com.smartlogix.order.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "coupon_redemptions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"customer_user_id", "coupon_code"}))
public class CouponRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "customer_email", nullable = false, length = 120)
    private String customerEmail;

    @Column(name = "coupon_code", nullable = false, length = 30)
    private String couponCode;

    @Column(nullable = false, length = 50)
    private String orderNumber;

    @Column(nullable = false)
    private OffsetDateTime usedAt;

    @PrePersist
    void onCreate() { usedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public Long getCustomerUserId() { return customerUserId; }
    public void setCustomerUserId(Long customerUserId) { this.customerUserId = customerUserId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public OffsetDateTime getUsedAt() { return usedAt; }
}
