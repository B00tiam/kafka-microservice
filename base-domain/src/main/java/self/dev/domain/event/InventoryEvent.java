package self.dev.domain.event;

import self.dev.domain.enums.InventoryStatus;

import java.time.LocalDateTime;
import java.util.Objects;


public class InventoryEvent {

    // ops: reserve/confirm/cancel

    // business info:
    private Long orderId;
    private Long inventoryId;
    private String productId;
    private Integer quantity;
    private Integer availableStock;     // current available stock

    // status info:
    private InventoryStatus status;

    // metadata:
    private LocalDateTime timestamp;
    private String reason;              // fail reason

    // Structure funcs
    public InventoryEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public InventoryEvent(Long orderId, Long inventoryId, String productId,
                          InventoryStatus status, Integer quantity) {
        this.orderId = orderId;
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.status = status;
        this.quantity = quantity;
        this.timestamp = LocalDateTime.now();
    }

    // Struct for failed inventory update
    public InventoryEvent(Long orderId, String productId, InventoryStatus status, String reason) {
        this.orderId = orderId;
        this.productId = productId;
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

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
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
        return "InventoryEvent{" +
                "orderId=" + orderId +
                ", inventoryId=" + inventoryId +
                ", productId='" + productId + '\'' +
                ", status=" + status +
                ", quantity=" + quantity +
                ", availableStock=" + availableStock +
                ", timestamp=" + timestamp +
                ", reason='" + reason + '\'' +
                '}';
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryEvent that = (InventoryEvent) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(inventoryId, that.inventoryId) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, inventoryId, timestamp);
    }
}
