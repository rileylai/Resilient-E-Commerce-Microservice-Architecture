package com.tut2.group3.bank.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableRabbit
@EnableConfigurationProperties(BankRabbitProperties.class)
public class RabbitMQConfig {

    @Bean
    public Queue bankEventsQueue(BankRabbitProperties properties) {
        return QueueBuilder.durable(properties.getQueueName()).build();
    }

    @Bean
    public Queue bankTransactionRequestQueue(BankRabbitProperties properties) {
        return QueueBuilder.durable(properties.getRequestQueue()).build();
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter,
                                         BankRabbitProperties properties) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setExchange(properties.resolvedExchangeName());
        rabbitTemplate.setRoutingKey(properties.resolvedRoutingKey());
        return rabbitTemplate;
    }
}
