package self.dev.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import self.dev.notification.domain.Notification;


public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByOrderId(Long orderId);
}
