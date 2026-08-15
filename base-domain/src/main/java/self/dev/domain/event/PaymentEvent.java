package self.dev.domain.event;

import self.dev.domain.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Objects;


public class PaymentEvent {

    // ops: reserve/confirm/cancel

    // business info:
    private Long orderId;
    private Long paymentId;
    private String customerId;
    private Double amount;              // payment amount

    // status info:
    private PaymentStatus status;

    // metadata:
    private LocalDateTime timestamp;
    private String reason;              // fail reason

    // Struct funcs
    public PaymentEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentEvent(Long orderId, Long paymentId, String customerId,
                        PaymentStatus status, Double amount) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.status = status;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    // Structure funcs for failed payment
    public PaymentEvent(Long orderId, PaymentStatus status, String reason) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // toString for debugging
    @Override
    public String toString() {
        return "PaymentEvent{" +
                "orderId=" + orderId +
                ", paymentId=" + paymentId +
                ", customerId='" + customerId + '\'' +
                ", status=" + status +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", reason='" + reason + '\'' +
                '}';
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentEvent that = (PaymentEvent) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(paymentId, that.paymentId) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, paymentId, timestamp);
    }
}