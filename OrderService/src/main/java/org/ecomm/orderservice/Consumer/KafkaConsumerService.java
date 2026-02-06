package org.ecomm.orderservice.Consumer;


import com.ecomm.grpc.payment.paymentStatusResponse;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.ecomm.orderservice.Service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
 class KafkaConsumerService {

    private final OrderService orderService;

    public KafkaConsumerService(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "payment-completed",
            groupId = "order-service-group"
    )
    public void handlePaymentCompleted(byte[] response) throws InvalidProtocolBufferException {
        try {
            paymentStatusResponse resp= paymentStatusResponse.parseFrom(response);

            log.info("Received payment completed event: {}", resp);

                orderService.confirmOrder(
                        resp.getOrderId(),
                        resp.getPaymentId()
                );

        }
        catch (InvalidProtocolBufferException e) {
           throw new InvalidProtocolBufferException(e.getMessage());
        }
        catch (Exception e) {
            log.error("Error processing payment completed event");
        }
    }

    @KafkaListener(
            topics = "payment-failed",
            groupId = "order-service-group"
    )
    public void handlePaymentFailed(byte[] response) throws InvalidProtocolBufferException {
        try {
            paymentStatusResponse resp= paymentStatusResponse.parseFrom(response);
            log.info("Received payment failed event: orderId={}",
                    resp.getOrderId());

            orderService.failOrder(resp.getOrderId(), "Payment failed");


        }
        catch (InvalidProtocolBufferException e){
            throw new InvalidProtocolBufferException(e.getMessage());
        }
        catch (Exception e) {
            log.error("Error processing payment failed event", e);
        }
    }
}