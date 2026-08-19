package self.dev.domain.enums;

public enum InventoryStatus {
    // 3 status: success, failed, pending
    RESERVATION_PENDING,
    RESERVATION_SUCCESS,
    RESERVATION_FAILED,

    // final check
    CONFIRMED,
    CANCELLED,

    // reasons for failed (later)
    // OUT_OF_STOCK,
    // INSUFFICIENT_STOCK,
}
