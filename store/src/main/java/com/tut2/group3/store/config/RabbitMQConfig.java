package com.tut2.group3.store.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ========== Exchange Names ==========
    public static final String DELIVERY_EXCHANGE = "delivery.exchange";
    public static final String STORE_EXCHANGE = "store.exchange";

    // ========== Queue Names ==========
    public static final String DELIVERY_REQUEST_QUEUE = "delivery.request.queue";
    public static final String DELIVERY_STATUS_QUEUE = "delivery.status.queue";
    public static final String ORDER_FAILURE_QUEUE = "email.orderfail.queue";
    public static final String REFUND_NOTIFICATION_QUEUE = "email.refund.queue";

    // ========== Routing Keys ==========
    public static final String DELIVERY_REQUEST_ROUTING_KEY = "delivery.request";
    public static final String DELIVERY_STATUS_ROUTING_KEY = "delivery.status.update";
    public static final String ORDER_FAILURE_ROUTING_KEY = "email.orderfail";
    public static final String REFUND_ROUTING_KEY = "email.refund";

    /**
     * Topic exchange for delivery-related messages
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
     */
    @Bean
    public TopicExchange storeExchange() {
        return ExchangeBuilder
                .topicExchange(STORE_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Queue for delivery requests
     */
    @Bean
    public Queue deliveryRequestQueue() {
        return QueueBuilder
                .durable(DELIVERY_REQUEST_QUEUE)
                .build();
    }

    /**
     * Queue for delivery status updates from DeliveryCo
     */
    @Bean
    public Queue deliveryStatusQueue() {
        return QueueBuilder
                .durable(DELIVERY_STATUS_QUEUE)
                .build();
    }

    /**
     * Queue for order failure notifications
     */
    @Bean
    public Queue orderFailureQueue() {
        return QueueBuilder
                .durable(ORDER_FAILURE_QUEUE)
                .build();
    }

    /**
     * Queue for refund notifications
     */
    @Bean
    public Queue refundNotificationQueue() {
        return QueueBuilder
                .durable(REFUND_NOTIFICATION_QUEUE)
                .build();
    }

    /**
     * Bind delivery request queue to delivery exchange
     */
    @Bean
    public Binding deliveryRequestBinding(Queue deliveryRequestQueue, TopicExchange deliveryExchange) {
        return BindingBuilder
                .bind(deliveryRequestQueue)
                .to(deliveryExchange)
                .with(DELIVERY_REQUEST_ROUTING_KEY);
    }

    /**
     * Bind delivery status queue to delivery exchange
     */
    @Bean
    public Binding deliveryStatusBinding(Queue deliveryStatusQueue, TopicExchange deliveryExchange) {
        return BindingBuilder
                .bind(deliveryStatusQueue)
                .to(deliveryExchange)
                .with(DELIVERY_STATUS_ROUTING_KEY);
    }

    /**
     * Bind order failure queue to store exchange
     */
    @Bean
    public Binding orderFailureBinding(Queue orderFailureQueue, TopicExchange storeExchange) {
        return BindingBuilder
                .bind(orderFailureQueue)
                .to(storeExchange)
                .with(ORDER_FAILURE_ROUTING_KEY);
    }

    /**
     * Bind refund notification queue to store exchange
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

