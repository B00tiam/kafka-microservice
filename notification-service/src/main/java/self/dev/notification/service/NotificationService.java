package self.dev.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import self.dev.domain.enums.NotificationTypes;
import self.dev.domain.enums.OrderStatus;
import self.dev.domain.event.OrderEvent;
import self.dev.notification.domain.Notification;
import self.dev.notification.repository.NotificationRepository;


@Service
public class NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationService.class);

    private static final String ORDERS_TOPIC = "orders";

    private final NotificationRepository notificationRepository;

    // Constructor
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // receive & publish final results
    @KafkaListener(topics = ORDERS_TOPIC, groupId = "notification-service")
    @Transactional
    public void handleOrderEvent(OrderEvent event) {

        if (event.getStatus() != OrderStatus.CONFIRMED
                && event.getStatus() != OrderStatus.CANCELLED) {
            return;
        }

        if (notificationRepository.existsByOrderId(event.getOrderId())) {
            return;
        }

        NotificationTypes type;
        String message;

        if (event.getStatus() == OrderStatus.CONFIRMED) {
            type = NotificationTypes.ORDER_CONFIRMED;
            message = "Order " + event.getOrderId() + " has been confirmed.";
        } else {
            type = NotificationTypes.ORDER_CANCELLED;

            String reason = event.getReason() == null ? "Unknown reason" : event.getReason();

            message = "Order " + event.getOrderId() + " has been cancelled. Reason: " + reason;
        }

        Notification notification = new Notification(
                event.getOrderId(),
                event.getCustomerId(),
                type,
                message
        );

        notificationRepository.save(notification);

        // Simulate sending a notification
        log.info(
                "Notification sent: customerId={}, type={}, message={}",
                event.getCustomerId(),
                type,
                message
        );
    }
}
