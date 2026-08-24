package self.dev.payment.domain;

import jakarta.persistence.*;


@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(nullable = false, updatable = false)
    private String customerId;

    @Column(nullable = false)
    private Double availableBalance;

    // balance that is reserved for pending transactions but still waiting for confirmation
    @Column(nullable = false)
    private Double reservedBalance;

    // Funcs:
    protected Account() {
        // JPA needs a default constructor
    }

    // Constructor for creating a new account with initial balances
    public Account(String customerId, Double availableBalance) {
        this.customerId = customerId;
        this.availableBalance = availableBalance;
        this.reservedBalance = 0.0;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Double getAvailableBalance() {
        return availableBalance;
    }

    public Double getReservedBalance() {
        return reservedBalance;
    }

    // Methods to update balances:
    // Reserve a certain amount from the available balance and add it to the reserved balance
    public void reserve(Double amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > availableBalance) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        availableBalance -= amount;
        reservedBalance += amount;
    }

    // Confirm a certain amount from the reserved balance, effectively finalizing the transaction
    public void confirm(Double amount) {
        if (amount == null || amount <= 0 || amount > reservedBalance) {
            throw new IllegalArgumentException("Invalid amount to confirm");
        }

        reservedBalance -= amount;
    }

    // Release a certain amount from the reserved balance back to the available balance
    public void release(Double amount) {
        if (amount == null || amount <= 0 || amount > reservedBalance) {
            throw new IllegalArgumentException("Invalid amount to release");
        }

        reservedBalance -= amount;
        availableBalance += amount;
    }

}
