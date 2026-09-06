package self.dev.saga;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.streams.KafkaStreams;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.client.RestClient;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import self.dev.order.OrderServiceApplication;
import self.dev.payment.PaymentServiceApplication;
import self.dev.inventory.InventoryServiceApplication;
import self.dev.notification.NotificationServiceApplication;


@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class SagaFlowIT {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka:latest")
    );

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgres:17")
            )
                    .withDatabaseName("postgres")
                    .withUsername("saga")
                    .withPassword("saga");

    @TempDir
    static Path streamsStateDirectory;

    private static final ParameterizedTypeReference<Map<String, Object>>
            JSON_OBJECT = new ParameterizedTypeReference<>() {};

    private final List<ConfigurableApplicationContext> applications =
            new ArrayList<>();

    private JdbcTemplate orderDb;
    private JdbcTemplate paymentDb;
    private JdbcTemplate inventoryDb;
    private JdbcTemplate notificationDb;

    private RestClient http;

    @BeforeAll
    void startEnvironment() throws Exception {
        createDatabases();
        createTopics();

        orderDb = database("order_db");
        paymentDb = database("payment_db");
        inventoryDb = database("inventory_db");
        notificationDb = database("notification_db");

        ConfigurableApplicationContext order = startApplication(
                OrderServiceApplication.class,
                "order-service",
                "order_db"
        );

        startApplication(
                PaymentServiceApplication.class,
                "payment-service",
                "payment_db"
        );

        startApplication(
                InventoryServiceApplication.class,
                "inventory-service",
                "inventory_db"
        );

        startApplication(
                NotificationServiceApplication.class,
                "notification-service",
                "notification_db"
        );

        waitUntilReady(order);

        int port = order.getEnvironment().getRequiredProperty(
                "local.server.port",
                Integer.class
        );

        http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private void createDatabases() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); var statement = connection.createStatement()) {

            // fixed names, only create in this temporary PostgreSQL container
            for (String name : List.of(
                    "order_db",
                    "payment_db",
                    "inventory_db",
                    "notification_db"
            )) {
                statement.executeUpdate("CREATE DATABASE " + name);
            }
        }
    }

    private void createTopics() throws Exception {
        try (Admin admin = Admin.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()
        ))) {
            admin.createTopics(List.of(
                    new NewTopic("orders", 3, (short) 1),
                    new NewTopic("payment-orders", 3, (short) 1),
                    new NewTopic("inventory-orders", 3, (short) 1)
            )).all().get(30, TimeUnit.SECONDS);
        }
    }

    private String jdbcUrl(String databaseName) {
        return "jdbc:postgresql://"
                + POSTGRES.getHost()
                + ":"
                + POSTGRES.getMappedPort(5432)
                + "/"
                + databaseName;
    }

    private JdbcTemplate database(String databaseName) {
        var dataSource = new DriverManagerDataSource(
                jdbcUrl(databaseName),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );

        return new JdbcTemplate(dataSource);
    }

    private ConfigurableApplicationContext startApplication(
            Class<?> applicationClass,
            String applicationName,
            String databaseName
    ) {
        ConfigurableApplicationContext context =
                new SpringApplicationBuilder(applicationClass)
                        .registerShutdownHook(false)
                        .run(
                                "--spring.config.location=" + "classpath:/saga-tests.yaml",

                                "--spring.application.name=" + applicationName,

                                "--spring.datasource.url=" + jdbcUrl(databaseName),

                                "--spring.datasource.username=" + POSTGRES.getUsername(),

                                "--spring.datasource.password=" + POSTGRES.getPassword(),

                                "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),

                                "--spring.kafka.streams.bootstrap-servers=" + KAFKA.getBootstrapServers(),

                                "--spring.kafka.streams.application-id=" + "saga-tests-order-streams",

                                "--spring.kafka.streams.state-dir=" + streamsStateDirectory.toAbsolutePath()
                        );

        applications.add(context);
        return context;
    }

    private void waitUntilReady(
            ConfigurableApplicationContext orderContext
    ) {
        StreamsBuilderFactoryBean streamsFactory =
                orderContext.getBean(
                        "&defaultKafkaStreamsBuilder",
                        StreamsBuilderFactoryBean.class
                );

        await()
                .alias("4 microservices' Kafka listener and Kafka Streams all started")
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> {
                    for (var application : applications) {
                        var registry = application.getBean(
                                KafkaListenerEndpointRegistry.class
                        );

                        assertFalse(
                                registry.getListenerContainers().isEmpty(),
                                application.getId() + " has no Kafka listener"
                        );

                        for (var listener :
                                registry.getListenerContainers()) {
                            assertTrue(listener.isRunning());

                            var partitions =
                                    listener.getAssignedPartitions();

                            assertNotNull(partitions);
                            assertFalse(partitions.isEmpty());
                        }
                    }

                    KafkaStreams streams = streamsFactory.getKafkaStreams();

                    assertNotNull(streams);
                    assertEquals(
                            KafkaStreams.State.RUNNING,
                            streams.state()
                    );
                });
    }

    @Test
    void shouldConfirmWhenPaymentAndInventorySucceed() {
        verifySaga(
                30.0,
                3,
                "CONFIRMED",
                70.0,
                7,
                null
        );
    }

    @Test
    void shouldReleaseInventoryWhenPaymentFails() {
        verifySaga(
                130.0,
                3,
                "CANCELLED",
                100.0,
                10,
                "Insufficient balance"
        );
    }

    @Test
    void shouldReleasePaymentWhenInventoryFails() {
        verifySaga(
                30.0,
                13,
                "CANCELLED",
                100.0,
                10,
                "Insufficient stock"
        );
    }

    @Test
    void shouldCancelWhenBothReservationsFail() {
        verifySaga(
                130.0,
                13,
                "CANCELLED",
                100.0,
                10,
                "Insufficient balance"
        );
    }

    private void verifySaga(
            double amount,
            int quantity,
            String expectedStatus,
            double expectedBalance,
            int expectedStock,
            String expectedReason
    ) {

        // independent data for each test case, do not delete other test cases' data
        String suffix = UUID.randomUUID().toString();
        String customerId = "customer-" + suffix;
        String productId = "product-" + suffix;

        paymentDb.update("""
                INSERT INTO accounts
                    (customer_id, available_balance, reserved_balance)
                VALUES (?, ?, ?)
                """,
                customerId,
                100.0,
                0.0
        );

        inventoryDb.update("""
                INSERT INTO stocks
                    (product_id, available_stock, reserved_stock, version)
                VALUES (?, ?, ?, ?)
                """,
                productId,
                10,
                0,
                0L
        );

        // only the real HTTP order submission, do not manually send the final Kafka event
        var response = http.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "customerId", customerId,
                        "productId", productId,
                        "quantity", quantity,
                        "amount", amount
                ))
                .retrieve()
                .toEntity(JSON_OBJECT);

        assertEquals(201, response.getStatusCode().value());

        Map<String, Object> created = response.getBody();
        assertNotNull(created);

        long orderId = ((Number) created.get("orderId")).longValue();

        String expectedNotificationType =
                expectedStatus.equals("CONFIRMED") ? "ORDER_CONFIRMED" : "ORDER_CANCELLED";

        await()
                .alias("Order " + orderId + " completed whole Saga")
                .pollInterval(Duration.ofMillis(200))
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> {
                    Map<String, Object> order = http.get()
                            .uri("/orders/{id}", orderId)
                            .retrieve()
                            .body(JSON_OBJECT);

                    assertNotNull(order);
                    assertEquals(
                            expectedStatus,
                            order.get("status"),
                            "HTTP query order status"
                    );
                    assertEquals(expectedReason, order.get("reason"));

                    assertEquals(
                            expectedStatus,
                            orderDb.queryForObject(
                                    "SELECT status FROM orders WHERE order_id = ?",
                                    String.class,
                                    orderId
                            ),
                            "Order database status"
                    );

                    assertEquals(
                            1L,
                            paymentDb.queryForObject(
                                    "SELECT COUNT(*) FROM payments WHERE order_id = ?",
                                    Long.class,
                                    orderId
                            )
                    );

                    assertEquals(
                            expectedStatus,
                            paymentDb.queryForObject(
                                    "SELECT status FROM payments WHERE order_id = ?",
                                    String.class,
                                    orderId
                            ),
                            "Payment final status"
                    );

                    assertEquals(
                            1L,
                            inventoryDb.queryForObject(
                                    "SELECT COUNT(*) FROM inventories WHERE order_id = ?",
                                    Long.class,
                                    orderId
                            )
                    );

                    assertEquals(
                            expectedStatus,
                            inventoryDb.queryForObject(
                                    "SELECT status FROM inventories WHERE order_id = ?",
                                    String.class,
                                    orderId
                            ),
                            "Inventory reservation final status"
                    );

                    assertEquals(
                            expectedBalance,
                            paymentDb.queryForObject(
                                    """
                                    SELECT available_balance
                                    FROM accounts
                                    WHERE customer_id = ?
                                    """,
                                    Double.class,
                                    customerId
                            ),
                            0.000001,
                            "Final available balance"
                    );

                    assertEquals(
                            0.0,
                            paymentDb.queryForObject(
                                    """
                                    SELECT reserved_balance
                                    FROM accounts
                                    WHERE customer_id = ?
                                    """,
                                    Double.class,
                                    customerId
                            ),
                            0.000001,
                            "Reserved funds should be zero"
                    );

                    assertEquals(
                            expectedStock,
                            inventoryDb.queryForObject(
                                    """
                                    SELECT available_stock
                                    FROM stocks
                                    WHERE product_id = ?
                                    """,
                                    Integer.class,
                                    productId
                            ).intValue(),
                            "Final available stock"
                    );

                    assertEquals(
                            0,
                            inventoryDb.queryForObject(
                                    """
                                    SELECT reserved_stock
                                    FROM stocks
                                    WHERE product_id = ?
                                    """,
                                    Integer.class,
                                    productId
                            ).intValue(),
                            "Inventory reservation should be zero"
                    );

                    assertEquals(
                            1L,
                            notificationDb.queryForObject(
                                    """
                                    SELECT COUNT(*)
                                    FROM notifications
                                    WHERE order_id = ?
                                    """,
                                    Long.class,
                                    orderId
                            ),
                            "One notification should be generated"
                    );

                    Map<String, Object> notification =
                            notificationDb.queryForMap(
                                    """
                                    SELECT customer_id, type, message
                                    FROM notifications
                                    WHERE order_id = ?
                                    """,
                                    orderId
                            );

                    assertEquals(
                            customerId,
                            notification.get("customer_id")
                    );

                    assertEquals(
                            expectedNotificationType,
                            notification.get("type")
                    );

                    if (expectedReason != null) {
                        assertTrue(
                                notification.get("message")
                                        .toString()
                                        .contains(expectedReason)
                        );
                    }
                });
    }

    @AfterAll
    void stopApplications() {
        // close app first, then Testcontainers will close Kafka and PostgreSQL
        // even if one app fails to close, continue to clean up other apps

        RuntimeException failure = null;

        for (int i = applications.size() - 1; i >= 0; i--) {
            try {
                applications.get(i).close();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        applications.clear();

        if (failure != null) {
            throw failure;
        }
    }
}