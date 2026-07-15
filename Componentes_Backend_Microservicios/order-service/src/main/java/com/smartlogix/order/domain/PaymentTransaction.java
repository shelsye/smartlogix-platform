package com.smartlogix.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private PurchaseOrder order;

    @Column(nullable = false, unique = true, length = 40)
    private String paymentReference;

    @Column(nullable = false, length = 30)
    private String authorizationCode;

    @Column(nullable = false, length = 30)
    private String cardBrand;

    @Column(nullable = false, length = 30)
    private String maskedCard;

    @Column(nullable = false, length = 120)
    private String cardHolderName;

    @Column(nullable = false)
    private int installments;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private OffsetDateTime paidAt;

    private OffsetDateTime refundedAt;

    public Long getId() { return id; }
    public PurchaseOrder getOrder() { return order; }
    public void setOrder(PurchaseOrder order) { this.order = order; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
    public String getMaskedCard() { return maskedCard; }
    public void setMaskedCard(String maskedCard) { this.maskedCard = maskedCard; }
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    public int getInstallments() { return installments; }
    public void setInstallments(int installments) { this.installments = installments; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
    public OffsetDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(OffsetDateTime refundedAt) { this.refundedAt = refundedAt; }
}
