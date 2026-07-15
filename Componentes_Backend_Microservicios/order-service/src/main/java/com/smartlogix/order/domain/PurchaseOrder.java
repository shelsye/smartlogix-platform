package com.smartlogix.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(nullable = false, length = 120)
    private String customerEmail;

    @Column(nullable = false, length = 255)
    private String shippingAddress;

    @Column(nullable = false, length = 80)
    private String shippingRegion;

    @Column(nullable = false, length = 30)
    private String shippingType;

    @Column(nullable = false, length = 80)
    private String shippingCarrier;

    @Column(length = 120)
    private String shippingRouteName;

    @Column(length = 50)
    private String shippingRouteCode;

    private Integer shippingEstimatedDays;

    private Integer shippingDistanceKm;

    private java.time.LocalDate shippingEstimatedDeliveryDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(length = 50)
    private String trackingCode;

    @Column(length = 250)
    private String rejectionReason;

    @Column(length = 30)
    private String couponCode;

    @Column(nullable = false)
    private boolean discountApplied;

    @Column(nullable = false)
    private int discountPercentage;

    @Column(nullable = false)
    private boolean stockFinalized;

    @Column(nullable = false)
    private boolean stockReleased;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderLine> lines = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private PaymentTransaction payment;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private ElectronicReceipt receipt;

    @PrePersist
    void createTimestamps() { createdAt = OffsetDateTime.now(); updatedAt = createdAt; }
    @PreUpdate
    void updateTimestamp() { updatedAt = OffsetDateTime.now(); }

    public void addLine(OrderLine line) { line.setOrder(this); lines.add(line); }
    public void clearLines() { lines.clear(); }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Long getCustomerUserId() { return customerUserId; }
    public void setCustomerUserId(Long customerUserId) { this.customerUserId = customerUserId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getShippingRegion() { return shippingRegion; }
    public void setShippingRegion(String shippingRegion) { this.shippingRegion = shippingRegion; }
    public String getShippingType() { return shippingType; }
    public void setShippingType(String shippingType) { this.shippingType = shippingType; }
    public String getShippingCarrier() { return shippingCarrier; }
    public void setShippingCarrier(String shippingCarrier) { this.shippingCarrier = shippingCarrier; }
    public String getShippingRouteName() { return shippingRouteName; }
    public void setShippingRouteName(String shippingRouteName) { this.shippingRouteName = shippingRouteName; }
    public String getShippingRouteCode() { return shippingRouteCode; }
    public void setShippingRouteCode(String shippingRouteCode) { this.shippingRouteCode = shippingRouteCode; }
    public Integer getShippingEstimatedDays() { return shippingEstimatedDays; }
    public void setShippingEstimatedDays(Integer shippingEstimatedDays) { this.shippingEstimatedDays = shippingEstimatedDays; }
    public Integer getShippingDistanceKm() { return shippingDistanceKm; }
    public void setShippingDistanceKm(Integer shippingDistanceKm) { this.shippingDistanceKm = shippingDistanceKm; }
    public java.time.LocalDate getShippingEstimatedDeliveryDate() { return shippingEstimatedDeliveryDate; }
    public void setShippingEstimatedDeliveryDate(java.time.LocalDate shippingEstimatedDeliveryDate) { this.shippingEstimatedDeliveryDate = shippingEstimatedDeliveryDate; }
    public BigDecimal getShippingPrice() { return shippingPrice; }
    public void setShippingPrice(BigDecimal shippingPrice) { this.shippingPrice = shippingPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public boolean isDiscountApplied() { return discountApplied; }
    public void setDiscountApplied(boolean discountApplied) { this.discountApplied = discountApplied; }
    public int getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(int discountPercentage) { this.discountPercentage = discountPercentage; }
    public boolean isStockFinalized() { return stockFinalized; }
    public void setStockFinalized(boolean stockFinalized) { this.stockFinalized = stockFinalized; }
    public boolean isStockReleased() { return stockReleased; }
    public void setStockReleased(boolean stockReleased) { this.stockReleased = stockReleased; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<OrderLine> getLines() { return lines; }
    public PaymentTransaction getPayment() { return payment; }
    public void setPayment(PaymentTransaction payment) {
        this.payment = payment;
        if (payment != null) payment.setOrder(this);
    }
    public ElectronicReceipt getReceipt() { return receipt; }
    public void setReceipt(ElectronicReceipt receipt) {
        this.receipt = receipt;
        if (receipt != null) receipt.setOrder(this);
    }
}
