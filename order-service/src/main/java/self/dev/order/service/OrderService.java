package self.dev.order.service;

import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

import self.dev.order.domain.Order;
import self.dev.order.repository.OrderRepository;
import self.dev.domain.enums.OrderStatus;
import self.dev.domain.event.OrderEvent;


@Service
public class OrderService {

    private static final String ORDERS_TOPIC = "orders";

    // init of order repository and kafka template
    private final OrderRepository orderRepository;
    private final KafkaTemplate<Long, OrderEvent> kafkaTemplate;

    // constructor function to inject dependencies
    public OrderService(OrderRepository orderRepository, KafkaTemplate<Long, OrderEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // method for operations
    public Order create(Order order) {
        Order savedOrder = orderRepository.save(order);

        OrderEvent event = toEvent(savedOrder);

        kafkaTemplate.send(
                ORDERS_TOPIC,
                savedOrder.getOrderId(),
                event
        );

        return savedOrder;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    // Receive final order results from payment service and update the order status accordingly
    @KafkaListener(topics = ORDERS_TOPIC, groupId = "order-service-results")
    @Transactional
    public void updateResult(OrderEvent event) {

        if (event.getStatus() != OrderStatus.CONFIRMED && event.getStatus() != OrderStatus.CANCELLED) {
            return;
        }

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found: " + event.getOrderId())
                );

        order.setStatus(event.getStatus());
        order.setReason(event.getReason());

        orderRepository.save(order);
    }


    // convert Order to OrderEvent
    private OrderEvent toEvent(Order order) {
        OrderEvent event = new OrderEvent(
                order.getOrderId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getProductId(),
                order.getQuantity(),
                order.getAmount()
        );

        event.setReason(order.getReason());
        return event;
    }

}
