package self.dev.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import self.dev.domain.event.*;
import self.dev.order.service.OrderManageService;

import java.time.Duration;


@Configuration
@EnableKafkaStreams
public class KafkaConfig {

    // create topics for orders, payment-orders, and inventory-orders with 3 partitions each
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders")
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic paymentOrdersTopic() {
        return TopicBuilder.name("payment-orders")
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic inventoryOrdersTopic() {
        return TopicBuilder.name("inventory-orders")
                .partitions(3)
                .build();
    }

    // combine the payment-orders and inventory-orders streams to produce the final order result stream
    @Bean
    public KStream<Long, OrderEvent> orderResultStream(StreamsBuilder builder, OrderManageService orderManageService) {

        JacksonJsonSerde<PaymentEvent> paymentSerde = new JacksonJsonSerde<>(PaymentEvent.class);

        JacksonJsonSerde<InventoryEvent> inventorySerde = new JacksonJsonSerde<>(InventoryEvent.class);

        JacksonJsonSerde<OrderEvent> orderSerde = new JacksonJsonSerde<>(OrderEvent.class);

        KStream<Long, PaymentEvent> paymentStream =
                builder.stream("payment-orders", Consumed.with(Serdes.Long(), paymentSerde));

        KStream<Long, InventoryEvent> inventoryStream =
                builder.stream("inventory-orders", Consumed.with(Serdes.Long(), inventorySerde));

        KStream<Long, OrderEvent> resultStream =
                paymentStream.join(
                        inventoryStream,
                        orderManageService::confirm,
                        JoinWindows.ofTimeDifferenceWithNoGrace(
                                Duration.ofSeconds(10)
                        ),
                        StreamJoined.with(
                                Serdes.Long(),
                                paymentSerde,
                                inventorySerde
                        )
                );

        resultStream.to("orders", Produced.with(Serdes.Long(), orderSerde));

        return resultStream;
    }

}
