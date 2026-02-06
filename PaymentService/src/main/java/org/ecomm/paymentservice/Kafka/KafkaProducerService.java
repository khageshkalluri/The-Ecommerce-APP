package org.ecomm.paymentservice.Kafka;

import com.ecomm.grpc.payment.paymentStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.ecomm.paymentservice.Model.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaProducerService {

    private KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Value("${kafka.topic.payment-completed}")
    String paymentCompletedTopic ;

    @Value("${kafka.topic.payment-failed}")
    String paymentFailedTopic ;

    public void sendPaymentCompletedEvent(Payment event) {
        try {

            paymentStatusResponse response = paymentStatusResponse.newBuilder()
                    .setPaymentId(event.getPaymentId())
                    .setStatus(event.getStatus().name())
                    .setAmount(event.getAmount())
                    .setOrderId(event.getOrderId()).build();

            log.info("Sending payment completed event: {}", response);

            this.kafkaTemplate.send(paymentCompletedTopic, response.toByteArray());

            log.info(" Event sent to topic: {}", paymentCompletedTopic);
        }
        catch (Exception e) {
            log.error("Error while sending payment completed event", e);
        }

    }

    public void sendPaymentFailedEvent(Payment event) {
        try {

            paymentStatusResponse response = paymentStatusResponse.newBuilder()
                    .setPaymentId(event.getPaymentId())
                    .setStatus(event.getStatus().name())
                    .setAmount(event.getAmount())
                    .setOrderId(event.getOrderId()).build();

            log.info("Sending payment failed event: {}", response);

            this.kafkaTemplate.send(paymentFailedTopic, response.toByteArray());

            log.info(" Event sent to topic: {}", paymentFailedTopic);
        }
        catch (Exception e) {
            log.error("Error while sending payment completed event", e);
        }

    }
}
