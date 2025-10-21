package com.tut2.group3.emailservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for EmailService
 *
 * Defines exchange, queues, and bindings for receiving email notification requests
 * from DeliveryCo and Store services.
 */
@Configuration
public class RabbitMQConfig {

    // ========== Exchange Names ==========
    public static final String DELIVERY_EXCHANGE = "delivery.exchange";
    public static final String STORE_EXCHANGE = "store.exchange";

    // ========== Queue Names ==========
    public static final String DELIVERY_EMAIL_QUEUE = "delivery.email.queue";
    public static final String ORDER_FAILURE_QUEUE = "email.orderfail.queue";
    public static final String REFUND_NOTIFICATION_QUEUE = "email.refund.queue";

    // ========== Routing Keys ==========
    public static final String DELIVERY_EMAIL_ROUTING_KEY = "notification.email";
    public static final String ORDER_FAILURE_ROUTING_KEY = "email.orderfail";
    public static final String REFUND_ROUTING_KEY = "email.refund";

    /**
     * Shared topic exchange for delivery messages
     *
     * NOTE: This exchange is primarily owned by DeliveryCo service.
     * We declare it here to ensure it exists when EmailService starts independently.
     * In production, consider having DeliveryCo create this exchange first.
     */
    @Bean
    public TopicExchange deliveryExchange() {
        return ExchangeBuilder
                .topicExchange(DELIVERY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Topic exchange for store-related messages
     *
     * NOTE: This exchange should ideally be created by Store service.
     * We declare it here temporarily to allow EmailService to start independently
     * before Store service is implemented.
     *
     * TODO: When Store service is implemented, consider:
     * 1. Move this exchange creation to Store service
     * 2. Or use a shared infrastructure service for exchange management
     */
    @Bean
    public TopicExchange storeExchange() {
        return ExchangeBuilder
                .topicExchange(STORE_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Queue for receiving delivery email notification requests
     */
    @Bean
    public Queue deliveryEmailQueue() {
        return QueueBuilder
                .durable(DELIVERY_EMAIL_QUEUE)
                .build();
    }

    /**
     * Queue for receiving order failure notifications
     */
    @Bean
    public Queue orderFailureQueue() {
        return QueueBuilder
                .durable(ORDER_FAILURE_QUEUE)
                .build();
    }

    /**
     * Queue for receiving refund notifications
     */
    @Bean
    public Queue refundNotificationQueue() {
        return QueueBuilder
                .durable(REFUND_NOTIFICATION_QUEUE)
                .build();
    }

    /**
     * Bind delivery email queue to delivery exchange with routing key
     */
    @Bean
    public Binding deliveryEmailBinding(Queue deliveryEmailQueue, TopicExchange deliveryExchange) {
        return BindingBuilder
                .bind(deliveryEmailQueue)
                .to(deliveryExchange)
                .with(DELIVERY_EMAIL_ROUTING_KEY);
    }

    /**
     * Bind order failure queue to store exchange with routing key
     */
    @Bean
    public Binding orderFailureBinding(Queue orderFailureQueue, TopicExchange storeExchange) {
        return BindingBuilder
                .bind(orderFailureQueue)
                .to(storeExchange)
                .with(ORDER_FAILURE_ROUTING_KEY);
    }

    /**
     * Bind refund notification queue to store exchange with routing key
     */
    @Bean
    public Binding refundNotificationBinding(Queue refundNotificationQueue, TopicExchange storeExchange) {
        return BindingBuilder
                .bind(refundNotificationQueue)
                .to(storeExchange)
                .with(REFUND_ROUTING_KEY);
    }

    /**
     * JSON message converter for automatic serialization/deserialization
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate configured with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
