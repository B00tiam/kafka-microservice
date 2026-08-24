package self.dev.payment.repository;

import self.dev.payment.domain.Payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);
}
