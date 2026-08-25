package self.dev.inventory.domain;

import self.dev.domain.enums.InventoryStatus;

import jakarta.persistence.*;


@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status;

    private String reason;

    // Funcs:
    protected Inventory() {
        // JPA needs a default constructor
    }

    public Inventory(Long orderId, String productId, Integer quantity, InventoryStatus status) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
