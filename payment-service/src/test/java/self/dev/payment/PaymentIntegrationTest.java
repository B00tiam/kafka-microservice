package self.dev.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import self.dev.domain.enums.OrderStatus;
import self.dev.domain.enums.PaymentStatus;
import self.dev.domain.event.OrderEvent;
import self.dev.payment.domain.Account;
import self.dev.payment.domain.Payment;
import self.dev.payment.repository.AccountRepository;
import self.dev.payment.repository.PaymentRepository;

import java.time.Duration;


@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaymentIntegrationTest {

    @Autowired
    private KafkaTemplate<Long, OrderEvent> kafkaTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void prepare() {
        paymentRepository.deleteAll();
        accountRepository.deleteAll();

        accountRepository.save(
                new Account("customer-1", 100.0)
        );
    }

    @Test
    void shouldReserveAndConfirmPayment() {
        long orderId = 1001L;

        OrderEvent created = new OrderEvent(
                orderId,
                "customer-1",
                OrderStatus.CREATED,
                "product-1",
                2,
                30.0
        );

        // sent to temp Kafka, let @KafkaListener use it
        kafkaTemplate.send("orders", orderId, created);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Payment payment = paymentRepository
                    .findByOrderId(orderId)
                    .orElseThrow();

            Account account = accountRepository
                    .findById("customer-1")
                    .orElseThrow();

            assertEquals(
                    PaymentStatus.RESERVATION_SUCCESS,
                    payment.getStatus()
            );
            assertEquals(70.0, account.getAvailableBalance());
            assertEquals(30.0, account.getReservedBalance());
        });

        OrderEvent confirmed = new OrderEvent(
                orderId,
                "customer-1",
                OrderStatus.CONFIRMED,
                "product-1",
                2,
                30.0
        );

        kafkaTemplate.send("orders", orderId, confirmed);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Payment payment = paymentRepository
                    .findByOrderId(orderId)
                    .orElseThrow();

            Account account = accountRepository
                    .findById("customer-1")
                    .orElseThrow();

            assertEquals(PaymentStatus.CONFIRMED, payment.getStatus());
            assertEquals(70.0, account.getAvailableBalance());
            assertEquals(0.0, account.getReservedBalance());
        });
    }
}