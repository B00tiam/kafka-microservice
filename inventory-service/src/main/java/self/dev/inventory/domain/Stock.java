package self.dev.inventory.domain;

import jakarta.persistence.*;


@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @Column(nullable = false, updatable = false)
    private String productId;

    @Column(nullable = false)
    private Integer availableStock;

    // stock that is reserved for pending transactions but still waiting for confirmation
    @Column(nullable = false)
    private Integer reservedStock;

    // Version field for optimistic locking
    @Version
    @Column(nullable = false)
    private Long version;

    // Funcs:
    protected Stock() {
        // JPA needs a default constructor
    }

    // Constructor for creating a new stock with initial quantities
    public Stock(String productId, Integer availableStock) {
        this.productId = productId;
        this.availableStock = availableStock;
        this.reservedStock = 0;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public Integer getReservedStock() {
        return reservedStock;
    }

    public Long getVersion() {
        return version;
    }

    // Methods to update stock quantities:
    // Reserve a certain quantity from the available stock and add it to the reserved stock
    public void reserve(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > availableStock) {
            throw new IllegalArgumentException("Insufficient stock");
        }

        availableStock -= quantity;
        reservedStock += quantity;
    }

    // Confirm a certain quantity from the reserved stock, effectively finalizing the transaction
    public void confirm(Integer quantity) {
        if (quantity == null || quantity <= 0 || quantity > reservedStock) {
            throw new IllegalArgumentException("Invalid quantity to confirm");
        }

        reservedStock -= quantity;
    }

    // Release a certain quantity from the reserved stock back to the available stock
    public void release(Integer quantity) {
        if (quantity == null || quantity <= 0 || quantity > reservedStock) {
            throw new IllegalArgumentException("Invalid quantity to release");
        }

        reservedStock -= quantity;
        availableStock += quantity;
    }

}