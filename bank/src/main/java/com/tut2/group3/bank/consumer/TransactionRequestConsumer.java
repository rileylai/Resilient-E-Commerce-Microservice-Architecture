package com.tut2.group3.bank.consumer;

import com.tut2.group3.bank.dto.TransactionRequestEventDTO;
import com.tut2.group3.bank.service.TransactionRequestHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRequestConsumer {

    private final TransactionRequestHandlerService handlerService;

    @RabbitListener(queues = "${bank.rabbitmq.request-queue}")
    public void consumeTransactionRequest(TransactionRequestEventDTO request) {
        log.info("Received transaction request eventType={} orderId={} userId={} idempotencyKey={}",
                request != null ? request.getEventType() : null,
                request != null ? request.getOrderId() : null,
                request != null ? request.getUserId() : null,
                request != null ? request.getIdempotencyKey() : null);
        try {
            // Handle incoming debit/refund request from Store
            handlerService.handleTransactionRequest(request);
        } catch (Exception ex) {
            log.error("Failed to process transaction request eventType={} orderId={} due to {}",
                    request != null ? request.getEventType() : null,
                    request != null ? request.getOrderId() : null,
                    ex.getMessage(), ex);
        }
    }
}
