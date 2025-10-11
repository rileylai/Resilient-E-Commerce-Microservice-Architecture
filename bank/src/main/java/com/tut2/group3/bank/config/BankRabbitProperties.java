package com.tut2.group3.bank.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "bank.rabbitmq")
public class BankRabbitProperties {

    @NotBlank
    private String queueName;

    private String exchangeName = "";

    private String routingKey;

    @NotBlank
    private String requestQueue;

    public String resolvedExchangeName() {
        return exchangeName == null ? "" : exchangeName;
    }

    public String resolvedRoutingKey() {
        return (routingKey == null || routingKey.isBlank()) ? queueName : routingKey;
    }

    public String resolvedRequestQueue() {
        return requestQueue;
    }
}
