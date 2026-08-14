package self.dev.domain.enums;

public enum OrderStatus {
    // init status
    CREATED,

    // operation for inventory & payment
    RESERVATION_PENDING,
    // reservation results
    PAYMENT_RESERVED,
    INVENTORY_RESERVED,
    PAYMENT_UNAVAILABLE,
    INVENTORY_UNAVAILABLE,

    // final check
    CONFIRMED,
    CANCELLED,

}
