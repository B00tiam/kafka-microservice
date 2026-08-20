package self.dev.order.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

import self.dev.order.domain.Order;
import self.dev.order.service.OrderService;


@RestController
@RequestMapping("/orders")
public class OrderController {

    // init of order service
    private final OrderService orderService;

    // constructor function to inject dependencies
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // create a new order (POST /orders)
    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest request) {

        Order order = new Order(
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.amount()
        );

        Order createdOrder = orderService.create(order);

        URI location = URI.create("/orders/" + createdOrder.getOrderId());

        return ResponseEntity.created(location).body(createdOrder);
    }

    // get all orders (GET /orders)
    @GetMapping
    public List<Order> findAll() {
        return orderService.findAll();
    }

    // get a specific order by ID (GET /orders/{orderId})
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> findById(
            @PathVariable Long orderId) {

        return ResponseEntity.of(
                orderService.findById(orderId)
        );
    }

    // request body for creating an order
    public record CreateOrderRequest(
            @NotBlank
            String customerId,

            @NotBlank
            String productId,

            @NotNull
            @Positive
            Integer quantity,

            @NotNull
            @Positive
            Double amount
    ) {
    }

}
