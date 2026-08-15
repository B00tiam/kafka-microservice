package self.dev.domain.event;

import self.dev.domain.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.Objects;


public class OrderEvent {
    
    // business info:
    private Long orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private Double amount;              // order amount
    
    // status info:
    private OrderStatus status;

    // metadata:
    private LocalDateTime timestamp;
    private String reason;              // fail reason
    
    // Structure funcs
    public OrderEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public OrderEvent(Long orderId, String customerId, OrderStatus status, 
                      String productId, Integer quantity, Double amount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
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
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
        return "OrderEvent{" +
                "orderId=" + orderId +
                ", customerId='" + customerId + '\'' +
                ", status=" + status +
                ", productId='" + productId + '\'' +
                ", quantity=" + quantity +
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
        OrderEvent that = (OrderEvent) o;
        return Objects.equals(orderId, that.orderId) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderId, timestamp);
    }
}

