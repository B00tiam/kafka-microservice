package self.dev.domain.enums;

public enum InventoryStatus {
    // 3 status: success, failed, pending
    RESERVTION_PENDING,
    RESERVTION_SUCCESS,
    RESERVTION_FAILED,

    // final check
    CONFIRMED,
    CANCELLED,

    // reasons for failed (later)
    // OUT_OF_STOCK,
    // INSUFFICIENT_STOCK,
}
