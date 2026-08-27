package self.dev.inventory;

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

import self.dev.domain.enums.InventoryStatus;
import self.dev.domain.enums.OrderStatus;
import self.dev.domain.event.OrderEvent;
import self.dev.inventory.domain.Inventory;
import self.dev.inventory.domain.Stock;
import self.dev.inventory.repository.InventoryRepository;
import self.dev.inventory.repository.StockRepository;


@Import(TestcontainersConfiguration.class)
@SpringBootTest(classes = InventoryServiceApplication.class)
class InventoryIntegrationTest {

    @Autowired
    private KafkaTemplate<Long, OrderEvent> kafkaTemplate;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void prepare() {
        inventoryRepository.deleteAll();
        stockRepository.deleteAll();

        stockRepository.save(
                new Stock("product-1", 100)
        );
    }

    @Test
    void shouldReserveAndConfirmInventory() {
        long orderId = 2001L;

        OrderEvent created = new OrderEvent(
                orderId,
                "customer-1",
                OrderStatus.CREATED,
                "product-1",
                3,
                30.0
        );

        kafkaTemplate.send("orders", orderId, created);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var inventoryOptional = inventoryRepository.findByOrderId(orderId);

            assertTrue(inventoryOptional.isPresent());

            Inventory inventory = inventoryOptional.get();
            Stock stock = stockRepository
                    .findById("product-1")
                    .orElseThrow();

            assertEquals(
                    InventoryStatus.RESERVATION_SUCCESS,
                    inventory.getStatus()
            );
            assertEquals(97, stock.getAvailableStock());
            assertEquals(3, stock.getReservedStock());
        });

        OrderEvent confirmed = new OrderEvent(
                orderId,
                "customer-1",
                OrderStatus.CONFIRMED,
                "product-1",
                3,
                30.0
        );

        kafkaTemplate.send("orders", orderId, confirmed);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Inventory inventory = inventoryRepository
                    .findByOrderId(orderId)
                    .orElseThrow();

            Stock stock = stockRepository
                    .findById("product-1")
                    .orElseThrow();

            assertEquals(
                    InventoryStatus.CONFIRMED,
                    inventory.getStatus()
            );
            assertEquals(97, stock.getAvailableStock());
            assertEquals(0, stock.getReservedStock());
        });
    }
}