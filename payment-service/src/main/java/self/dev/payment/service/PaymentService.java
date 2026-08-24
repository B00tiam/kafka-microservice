package self.dev.payment.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import self.dev.domain.enums.OrderStatus;
import self.dev.domain.enums.PaymentStatus;
import self.dev.domain.event.OrderEvent;
import self.dev.domain.event.PaymentEvent;
import self.dev.payment.domain.*;
import self.dev.payment.repository.*;


@Service
public class PaymentService {

    private static final String ORDERS_TOPIC = "orders";
    private static final String PAYMENT_ORDERS_TOPIC = "payment-orders";

    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<Long, PaymentEvent> kafkaTemplate;

    // Constructor injection for dependencies
    public PaymentService(
            AccountRepository accountRepository,
            PaymentRepository paymentRepository,
            KafkaTemplate<Long, PaymentEvent> kafkaTemplate) {
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Receive both newly created orders and final order results
    @KafkaListener(topics = ORDERS_TOPIC, groupId = "payment-service")
    @Transactional
    public void handleOrderEvent(OrderEvent event) {

        if (event.getStatus() == null) {
            return;
        }

        switch (event.getStatus()) {
            case CREATED -> reserve(event);
            case CONFIRMED -> confirm(event.getOrderId());
            case CANCELLED -> cancel(event.getOrderId());
            default -> {
                // Ignore statuses unrelated to payment processing
            }
        }
    }

    // Reserve funds when a new order is created
    private void reserve(OrderEvent event) {

        Optional<Payment> existingPayment =
                paymentRepository.findByOrderId(event.getOrderId());

        // Prevent duplicate fund reservation
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();

            // Resend the reservation result when Kafka redelivers the message
            if (payment.getStatus() == PaymentStatus.RESERVATION_SUCCESS
                    || payment.getStatus() == PaymentStatus.RESERVATION_FAILED) {
                publishResult(payment);
            }

            return;
        }

        if (event.getAmount() == null || event.getAmount() <= 0) {
            saveFailure(event, "Invalid payment amount");
            return;
        }

        Optional<Account> accountOptional =
                accountRepository.findById(event.getCustomerId());

        if (accountOptional.isEmpty()) {
            saveFailure(event, "Account not found");
            return;
        }

        Account account = accountOptional.get();

        if (account.getAvailableBalance() < event.getAmount()) {
            saveFailure(event, "Insufficient balance");
            return;
        }

        account.reserve(event.getAmount());
        accountRepository.save(account);

        Payment payment = new Payment(
                event.getOrderId(),
                event.getCustomerId(),
                event.getAmount(),
                PaymentStatus.RESERVATION_SUCCESS
        );

        Payment savedPayment = paymentRepository.save(payment);

        publishResult(savedPayment);
    }

    // Finalize the reserved funds
    private void confirm(Long orderId) {

        Optional<Payment> paymentOptional =
                paymentRepository.findByOrderId(orderId);

        if (paymentOptional.isEmpty()) {
            return;
        }

        Payment payment = paymentOptional.get();

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.RESERVATION_SUCCESS) {
            return;
        }

        Account account = accountRepository
                .findById(payment.getCustomerId())
                .orElseThrow(() ->
                        new IllegalStateException("Account not found: " + payment.getCustomerId())
                );

        account.confirm(payment.getAmount());
        payment.setStatus(PaymentStatus.CONFIRMED);

        accountRepository.save(account);
        paymentRepository.save(payment);
    }

    // Release reserved funds when the order is cancelled
    private void cancel(Long orderId) {

        Optional<Payment> paymentOptional = paymentRepository.findByOrderId(orderId);

        if (paymentOptional.isEmpty()) {
            return;
        }

        Payment payment = paymentOptional.get();

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return;
        }

        if (payment.getStatus() == PaymentStatus.RESERVATION_SUCCESS) {
            Account account = accountRepository
                    .findById(payment.getCustomerId())
                    .orElseThrow(() ->
                            new IllegalStateException("Account not found: " + payment.getCustomerId())
                    );

            account.release(payment.getAmount());
            accountRepository.save(account);
        } else if (payment.getStatus()
                != PaymentStatus.RESERVATION_FAILED) {
            return;
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
    }

    // Save an expected business failure and publish its result
    private void saveFailure(OrderEvent event, String reason) {

        Payment payment = new Payment(
                event.getOrderId(),
                event.getCustomerId(),
                event.getAmount(),
                PaymentStatus.RESERVATION_FAILED
        );

        payment.setReason(reason);

        Payment savedPayment = paymentRepository.save(payment);

        publishResult(savedPayment);
    }

    // Publish the payment reservation result
    private void publishResult(Payment payment) {

        PaymentEvent event = new PaymentEvent(
                payment.getOrderId(),
                payment.getPaymentId(),
                payment.getCustomerId(),
                payment.getStatus(),
                payment.getAmount()
        );

        event.setReason(payment.getReason());

        kafkaTemplate.send(
                PAYMENT_ORDERS_TOPIC,
                payment.getOrderId(),
                event
        );
    }

}
