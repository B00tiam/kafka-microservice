package self.dev.payment;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import self.dev.domain.enums.OrderStatus;
import self.dev.domain.enums.PaymentStatus;
import self.dev.domain.event.OrderEvent;
import self.dev.payment.domain.Account;
import self.dev.payment.domain.Payment;
import self.dev.payment.repository.AccountRepository;
import self.dev.payment.repository.PaymentRepository;


@Import(TestcontainersConfiguration.class)
@SpringBootTest(classes = PaymentServiceApplication.class)
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
            var paymentOptional = paymentRepository.findByOrderId(orderId);

            assertTrue(paymentOptional.isPresent());

            Payment payment = paymentOptional.get();
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

    @Test
    void shouldRejectPaymentWhenBalanceIsInsufficient() {
        long orderId = 1002L;

        kafkaTemplate.send(
                "orders",
                orderId,
                paymentEvent(orderId, OrderStatus.CREATED, 130.0)
        );

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var paymentOptional =
                    paymentRepository.findByOrderId(orderId);

            assertTrue(paymentOptional.isPresent());

            Payment payment = paymentOptional.get();
            Account account = accountRepository
                    .findById("customer-1")
                    .orElseThrow();

            assertEquals(
                    PaymentStatus.RESERVATION_FAILED,
                    payment.getStatus()
            );
            assertEquals("Insufficient balance", payment.getReason());
            assertEquals(100.0, account.getAvailableBalance());
            assertEquals(0.0, account.getReservedBalance());
        });
    }

    @Test
    void shouldCancelAndReleaseReservedPayment() {
        long orderId = 1003L;

        kafkaTemplate.send(
                "orders",
                orderId,
                paymentEvent(orderId, OrderStatus.CREATED, 30.0)
        );

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var paymentOptional =
                    paymentRepository.findByOrderId(orderId);

            assertTrue(paymentOptional.isPresent());
            assertEquals(
                    PaymentStatus.RESERVATION_SUCCESS,
                    paymentOptional.get().getStatus()
            );
        });

        kafkaTemplate.send(
                "orders",
                orderId,
                paymentEvent(orderId, OrderStatus.CANCELLED, 30.0)
        );

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var paymentOptional =
                    paymentRepository.findByOrderId(orderId);

            assertTrue(paymentOptional.isPresent());

            Account account = accountRepository
                    .findById("customer-1")
                    .orElseThrow();

            assertEquals(
                    PaymentStatus.CANCELLED,
                    paymentOptional.get().getStatus()
            );
            assertEquals(100.0, account.getAvailableBalance());
            assertEquals(0.0, account.getReservedBalance());
        });
    }

    @Test
    void shouldNotReserveTwiceWhenCreatedEventIsDuplicated() {
        long orderId = 1004L;
        OrderEvent created =
                paymentEvent(orderId, OrderStatus.CREATED, 30.0);

        kafkaTemplate.send("orders", orderId, created);
        kafkaTemplate.send("orders", orderId, created);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var paymentOptional =
                    paymentRepository.findByOrderId(orderId);

            assertTrue(paymentOptional.isPresent());
            assertEquals(
                    PaymentStatus.RESERVATION_SUCCESS,
                    paymentOptional.get().getStatus()
            );
        });

        /*
         * 3 messages are sent with the same Kafka key, so they will be processed in order.
         * When CANCELLED is processed, the two previous CREATED events have already been processed.
         */
        kafkaTemplate.send(
                "orders",
                orderId,
                paymentEvent(orderId, OrderStatus.CANCELLED, 30.0)
        );

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var paymentOptional =
                    paymentRepository.findByOrderId(orderId);

            assertTrue(paymentOptional.isPresent());

            Account account = accountRepository
                    .findById("customer-1")
                    .orElseThrow();

            assertEquals(1, paymentRepository.count());
            assertEquals(
                    PaymentStatus.CANCELLED,
                    paymentOptional.get().getStatus()
            );
            assertEquals(100.0, account.getAvailableBalance());
            assertEquals(0.0, account.getReservedBalance());
        });
    }

    private OrderEvent paymentEvent(
            long orderId,
            OrderStatus status,
            double amount) {

        return new OrderEvent(
                orderId,
                "customer-1",
                status,
                "product-1",
                2,
                amount
        );
    }
}