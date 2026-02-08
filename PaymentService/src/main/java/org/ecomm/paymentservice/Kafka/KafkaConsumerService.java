package org.ecomm.paymentservice.Kafka;

import com.ecomm.grpc.payment.paymentRequest;
import com.ecomm.grpc.payment.paymentResponse;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.ecomm.paymentservice.Model.Payment;
import org.ecomm.paymentservice.Service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class KafkaConsumerService {

    private PaymentService paymentService;
    private KafkaProducerService kafkaProducerService;

    @Value("${razorpay.enabled}")
    private boolean paymentEnabled;

    public KafkaConsumerService(PaymentService paymentService, KafkaProducerService kafkaProducerService) {
        this.paymentService = paymentService;
        this.kafkaProducerService = kafkaProducerService;
    }


    @KafkaListener(
            topics = "payment-creation-failed",
            groupId = "payment-service-group"
    )
    public void handlePaymentCreationFailed(byte[] paymentReq) throws InvalidProtocolBufferException {

        paymentRequest request = paymentRequest.parseFrom(paymentReq);

        log.info("Received payment Creation Failed Event for order: {}", request.getOrderId());
        if(paymentEnabled) {
            try {
                Payment payment = paymentService.createPayment(
                        request.getOrderId(),
                        request.getAmount(),
                        request.getCurrency(),
                        request.getCustomerEmail(),
                        request.getCustomerPhone()
                );

                this.kafkaProducerService.sendPaymentCompletedEvent(payment);
                log.info("Payment response sent: {}", payment.getPaymentId());


            } catch (Exception e) {
                log.error("Error processing payment", e);
            }
        }
        else{

            Payment payment = Payment.builder()
                    .paymentId(UUID.randomUUID().toString())
                    .amount(request.getAmount())
                    .status(Payment.PaymentStatus.SUCCESS)
                    .orderId(request.getOrderId())
                    .build();

            this.kafkaProducerService.sendPaymentCompletedEvent(payment);

            log.info("Payment response sent");
        }
    }
}
