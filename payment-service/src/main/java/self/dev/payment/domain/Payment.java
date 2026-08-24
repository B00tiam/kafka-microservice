package self.dev.payment.domain;

import self.dev.domain.enums.PaymentStatus;

import jakarta.persistence.*;


@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String reason;

    // Funcs:
    protected Payment() {
        // JPA needs a default constructor
    }

    public Payment(Long orderId, String customerId, Double amount, PaymentStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Double getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
