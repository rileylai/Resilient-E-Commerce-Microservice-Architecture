package com.tut2.group3.warehouse.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String WAREHOUSE_EXCHANGE = "warehouse.exchange";
    public static final String DELIVERY_EXCHANGE = "delivery.exchange";
    public static final String ORDER_EXCHANGE = "order.exchange";

    // Queue names
    public static final String WAREHOUSE_DELIVERY_PICKUP_QUEUE = "warehouse.delivery.pickup";
    public static final String WAREHOUSE_ORDER_CANCELLED_QUEUE = "warehouse.order.cancelled";

    // Routing keys
    public static final String STOCK_RESERVED_ROUTING_KEY = "warehouse.stock.reserved";
    public static final String STOCK_CONFIRMED_ROUTING_KEY = "warehouse.stock.confirmed";
    public static final String STOCK_RELEASED_ROUTING_KEY = "warehouse.stock.released";
    public static final String DELIVERY_PICKUP_CONFIRMED_ROUTING_KEY = "delivery.pickup.confirmed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    /**
     * Warehouse exchange - for publishing events
     */
    @Bean
    public TopicExchange warehouseExchange() {
        return new TopicExchange(WAREHOUSE_EXCHANGE, true, false);
    }

    /**
     * Queue for delivery pickup confirmation events
     */
    @Bean
    public Queue deliveryPickupQueue() {
        return QueueBuilder.durable(WAREHOUSE_DELIVERY_PICKUP_QUEUE)
                .withArgument("x-dead-letter-exchange", WAREHOUSE_EXCHANGE + ".dlx")
                .build();
    }

    /**
     * Queue for order cancelled events
     */
    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(WAREHOUSE_ORDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", WAREHOUSE_EXCHANGE + ".dlx")
                .build();
    }

    /**
     * Dead letter exchange for failed messages
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(WAREHOUSE_EXCHANGE + ".dlx", true, false);
    }

    /**
     * Dead letter queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(WAREHOUSE_EXCHANGE + ".dlq").build();
    }

    /**
     * Bind dead letter queue to dead letter exchange
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("#");
    }

    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(DELIVERY_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Binding deliveryPickupBinding() {
        return BindingBuilder.bind(deliveryPickupQueue())
                .to(deliveryExchange())
                .with(DELIVERY_PICKUP_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderCancelledQueue())
                .to(orderExchange())
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
