package self.dev.domain.enums;

public enum PaymentStatus {
    // 3 status: success, failed, pending
    RESERVATION_PENDING,
    RESERVATION_SUCCESS,
    RESERVATION_FAILED,

    // final check
    CONFIRMED,
    CANCELLED,

    // reasons for failed (later)
    // INSUFFICIENT_BALANCE,
    // ACCOUNT_LOCKED,
    // TIMEOUT,
}
