package com.tut2.group3.deliveryco.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for DeliveryCo Service
 *
 * Defines exchange, queues, and bindings for message communication
 * with Store service and EmailService.
 */
@Configuration
public class RabbitMQConfig {

    // ========== Exchange Names ==========
    public static final String DELIVERY_EXCHANGE = "delivery.exchange";

    // ========== Queue Names ==========
    public static final String DELIVERY_REQUEST_QUEUE = "delivery.request.queue";
    public static final String DELIVERY_CANCELLATION_QUEUE = "delivery.cancellation.queue";
    public static final String DELIVERY_STATUS_QUEUE = "delivery.status.queue";
    public static final String DELIVERY_EMAIL_QUEUE = "delivery.email.queue";

    // ========== Routing Keys ==========
    public static final String DELIVERY_REQUEST_ROUTING_KEY = "delivery.request";
    public static final String DELIVERY_CANCELLATION_ROUTING_KEY = "delivery.cancellation";
    public static final String DELIVERY_STATUS_ROUTING_KEY = "delivery.status.update";
    public static final String DELIVERY_EMAIL_ROUTING_KEY = "notification.email";

    @Bean
    public TopicExchange deliveryExchange() {
        return ExchangeBuilder
                .topicExchange(DELIVERY_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue deliveryRequestQueue() {
        return QueueBuilder
                .durable(DELIVERY_REQUEST_QUEUE)
                .build();
    }

    @Bean
    public Queue deliveryCancellationQueue() {
        return QueueBuilder
                .durable(DELIVERY_CANCELLATION_QUEUE)
                .build();
    }

    @Bean
    public Queue deliveryStatusQueue() {
        return QueueBuilder
                .durable(DELIVERY_STATUS_QUEUE)
                .build();
    }

    @Bean
    public Queue deliveryEmailQueue() {
        return QueueBuilder
                .durable(DELIVERY_EMAIL_QUEUE)
                .build();
    }

    @Bean
    public Binding deliveryRequestBinding(Queue deliveryRequestQueue, TopicExchange deliveryExchange) {
        return BindingBuilder
                .bind(deliveryRequestQueue)
                .to(deliveryExchange)
                .with(DELIVERY_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding deliveryCancellationBinding(Queue deliveryCancellationQueue, TopicExchange deliveryExchange) {
        return BindingBuilder
                .bind(deliveryCancellationQueue)
                .to(deliveryExchange)
                .with(DELIVERY_CANCELLATION_ROUTING_KEY);
    }

    @Bean
    public Binding deliveryStatusBinding(Queue deliveryStatusQueue, TopicExchange deliveryExchange) {
        return BindingBuilder
                .bind(deliveryStatusQueue)
                .to(deliveryExchange)
                .with(DELIVERY_STATUS_ROUTING_KEY);
    }

    @Bean
    public Binding deliveryEmailBinding(Queue deliveryEmailQueue, TopicExchange deliveryExchange) {
        return BindingBuilder
                .bind(deliveryEmailQueue)
                .to(deliveryExchange)
                .with(DELIVERY_EMAIL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}