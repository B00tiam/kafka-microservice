package self.dev.domain.event;

import self.dev.domain.enums.NotificationTypes;

import java.time.LocalDateTime;
import java.util.Objects;


public class NotificationEvent {

    // business info:
    private Long orderId;
    private String customerId;

    // notification info:
    private NotificationTypes type;
    private String message;                 // msg content

    // metadata:
    private LocalDateTime timestamp;

    // Structure funcs
    public NotificationEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public NotificationEvent(Long orderId, String customerId,
                             NotificationTypes type, String message) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.type = type;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public NotificationTypes getType() {
        return type;
    }

    public void setType(NotificationTypes type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // toString for debugging
    @Override
    public String toString() {
        return "NotificationEvent{" +
                "orderId=" + orderId +
                ", customerId='" + customerId + '\'' +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationEvent that = (NotificationEvent) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, timestamp);
    }
}
