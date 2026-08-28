package self.dev.notification.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import self.dev.domain.enums.NotificationTypes;


@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTypes type;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Funcs:
    protected Notification() {
        // JPA needs a default constructor
    }

    public Notification(
            Long orderId,
            String customerId,
            NotificationTypes type,
            String message) {

        this.orderId = orderId;
        this.customerId = customerId;
        this.type = type;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    // getters
    public Long getNotificationId() {
        return notificationId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public NotificationTypes getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}