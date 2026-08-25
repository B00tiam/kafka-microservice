package self.dev.inventory.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import self.dev.domain.enums.OrderStatus;
import self.dev.domain.enums.InventoryStatus;
import self.dev.domain.event.OrderEvent;
import self.dev.domain.event.InventoryEvent;
import self.dev.inventory.domain.*;
import self.dev.inventory.repository.*;


@Service
public class InventoryService {

    private static final String ORDERS_TOPIC = "orders";
    private static final String INVENTORY_ORDERS_TOPIC = "inventory-orders";

    private final StockRepository stockRepository;
    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<Long, InventoryEvent> kafkaTemplate;

    // Constructor injection for dependencies
    public InventoryService(
            StockRepository stockRepository,
            InventoryRepository inventoryRepository,
            KafkaTemplate<Long, InventoryEvent> kafkaTemplate) {
        this.stockRepository = stockRepository;
        this.inventoryRepository = inventoryRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Receive both newly created orders and final order results
    @KafkaListener(topics = ORDERS_TOPIC, groupId = "inventory-service")
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
                // Ignore statuses unrelated to inventory processing
            }
        }
    }

    // Reserve stock when a new order is created
    private void reserve(OrderEvent event) {

        Optional<Inventory> existingInventory =
                inventoryRepository.findByOrderId(event.getOrderId());

        // Prevent duplicate processing of the same order
        if (existingInventory.isPresent()) {
            Inventory inventory = existingInventory.get();

            // Resend the reservation result when Kafka redelivers the message
            if (inventory.getStatus() == InventoryStatus.RESERVATION_SUCCESS
                    || inventory.getStatus() == InventoryStatus.RESERVATION_FAILED) {
                publishResult(inventory);
            }

            return;
        }

        // If the order quantity is invalid, save a failure and return
        if (event.getQuantity() == null || event.getQuantity() <= 0) {
            saveFailure(event, "Invalid inventory quantity");
            return;
        }

        // If the product ID is invalid, save a failure and return
        if (event.getProductId() == null || event.getProductId().isBlank()) {
            saveFailure(event, "Invalid product ID");
            return;
        }

        Optional<Stock> stockOptional =
                stockRepository.findById(event.getProductId());

        // If the stock is not found, save a failure and return
        if (stockOptional.isEmpty()) {
            saveFailure(event, "Stock not found");
            return;
        }

        Stock stock = stockOptional.get();

        // If the available stock is insufficient, save a failure and return
        if (stock.getAvailableStock() < event.getQuantity()) {
            saveFailure(event, "Insufficient stock");
            return;
        }

        stock.reserve(event.getQuantity());
        stockRepository.save(stock);

        Inventory inventory = new Inventory(
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity(),
                InventoryStatus.RESERVATION_SUCCESS
        );

        Inventory savedInventory = inventoryRepository.save(inventory);

        publishResult(savedInventory);
    }

    // Finalize the stock reservation when the order is confirmed
    private void confirm(Long orderId) {

        Optional<Inventory> inventoryOptional =
                inventoryRepository.findByOrderId(orderId);

        if (inventoryOptional.isEmpty()) {
            return;
        }

        Inventory inventory = inventoryOptional.get();

        // If the inventory is already confirmed / failed, no further action is needed
        if (inventory.getStatus() == InventoryStatus.CONFIRMED) {
            return;
        }

        if (inventory.getStatus() != InventoryStatus.RESERVATION_SUCCESS) {
            return;
        }

        Stock stock = stockRepository
                .findById(inventory.getProductId())
                .orElseThrow(() ->
                        new IllegalStateException("Stock not found: " + inventory.getProductId())
                );

        stock.confirm(inventory.getQuantity());
        inventory.setStatus(InventoryStatus.CONFIRMED);

        // Save the updated stock and inventory status
        stockRepository.save(stock);
        inventoryRepository.save(inventory);
    }

    // Release reserved stock when the order is cancelled
    private void cancel(Long orderId) {

        Optional<Inventory> inventoryOptional = inventoryRepository.findByOrderId(orderId);

        // If the inventory is not found, no further action is needed
        if (inventoryOptional.isEmpty()) {
            return;
        }

        Inventory inventory = inventoryOptional.get();

        if (inventory.getStatus() == InventoryStatus.CANCELLED) {
            return;
        }

        if (inventory.getStatus() == InventoryStatus.RESERVATION_SUCCESS) {
            Stock stock = stockRepository
                    .findById(inventory.getProductId())
                    .orElseThrow(() ->
                            new IllegalStateException("Stock not found: " + inventory.getProductId())
                    );

            // Release the reserved stock back to available stock
            stock.release(inventory.getQuantity());
            stockRepository.save(stock);
        } else if (inventory.getStatus()
                != InventoryStatus.RESERVATION_FAILED) {
            // If the inventory is in an unexpected state, no further action is needed
            return;
        }

        inventory.setStatus(InventoryStatus.CANCELLED);
        // Save the updated inventory status
        inventoryRepository.save(inventory);
    }

    // Save an expected business failure and publish its result
    private void saveFailure(OrderEvent event, String reason) {

        Inventory inventory = new Inventory(
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity(),
                InventoryStatus.RESERVATION_FAILED
        );

        inventory.setReason(reason);

        Inventory savedInventory = inventoryRepository.save(inventory);

        publishResult(savedInventory);
    }

    // Publish the payment reservation result
    private void publishResult(Inventory inventory) {

        InventoryEvent event = new InventoryEvent(
                inventory.getOrderId(),
                inventory.getInventoryId(),
                inventory.getProductId(),
                inventory.getStatus(),
                inventory.getQuantity()
        );

        event.setReason(inventory.getReason());

        kafkaTemplate.send(
                INVENTORY_ORDERS_TOPIC,
                inventory.getOrderId(),
                event
        );
    }

}
