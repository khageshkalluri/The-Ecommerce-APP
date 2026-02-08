package org.ecomm.orderservice.Producer;


import com.ecomm.grpc.payment.OrderEvent;
import com.ecomm.grpc.payment.paymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecomm.orderservice.Entity.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-confirmed}")
    private String orderConfirmedTopic;

    @Value("${kafka.topics.order-failed}")
    private String orderFailedTopic;

    @Value("${kafka.topics.payment-creation-failed}")
    private String paymentCreationFailedEvent;

    public void publishOrderCreated(OrderEvent event) {
        OrderEvent.newBuilder().setOrderId(UUID.randomUUID().toString());
        publishEvent(orderCreatedTopic, event);
    }

    public void publishOrderConfirmed(OrderEvent event) {
        OrderEvent.newBuilder().setOrderId(UUID.randomUUID().toString());
        publishEvent(orderConfirmedTopic, event);
    }

    public void publishOrderFailed(OrderEvent event) {
        OrderEvent.newBuilder().setOrderId(UUID.randomUUID().toString());
        publishEvent(orderFailedTopic, event);
    }

    public void publishPaymentFailed(paymentRequest event) {
        OrderEvent.newBuilder().setOrderId(UUID.randomUUID().toString());
        publishEvent(paymentCreationFailedEvent, event);
    }

    private void publishEvent(String topic, OrderEvent event) {
        CompletableFuture<SendResult<String, byte[]>> future =
                kafkaTemplate.send(topic, event.getOrderId(), event.toByteArray());

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published event: topic={}, eventType={}, orderId={}, offset={}",
                        topic,
                        event.getEventType(),
                        event.getOrderId(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event: topic={}, orderId={}",
                        topic, event.getOrderId(), ex);
            }
        });
    }

    private void publishEvent(String topic, paymentRequest event) {
        CompletableFuture<SendResult<String, byte[]>> future =
                kafkaTemplate.send(topic, event.getOrderId(), event.toByteArray());

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published event: topic={}, eventType={}, orderId={}",
                        topic,
                        paymentCreationFailedEvent,
                        event.getOrderId());
            } else {
                log.error("Failed to publish event: topic={}, orderId={}",
                        topic, event.getOrderId(), ex);
            }
        });
    }
}
