package org.ecomm.orderservice.GrpcClientService;


import com.ecomm.grpc.payment.PaymentServiceGrpc;
import com.ecomm.grpc.payment.paymentRequest;
import com.ecomm.grpc.payment.paymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.ecomm.orderservice.Entity.Order;
import org.ecomm.orderservice.Entity.OrderStatus;
import org.ecomm.orderservice.Producer.KafkaProducerService;
import org.ecomm.orderservice.Repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class GrpcClientService {

  private final PaymentServiceGrpc.PaymentServiceBlockingStub blockingStubstub;
  private final OrderRepository orderRepository;
  private final KafkaProducerService kafkaProducerService;

  public GrpcClientService(@Value("${payment.server.address:localhost}") String serverAddress,@Value("${payment.server.port:9001}") int port,OrderRepository orderRepository,KafkaProducerService kafkaProducerService) {

    ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(serverAddress, port).usePlaintext().build();
    this.blockingStubstub = PaymentServiceGrpc.newBlockingStub(managedChannel);
    this.orderRepository = orderRepository;
    this.kafkaProducerService = kafkaProducerService;
  }

@CircuitBreaker(name="paymentServiceCircuitBreaker",fallbackMethod = "paymentServiceCircuitBreakerFallback")
@Retry(name = "paymentServiceCircuitBreakerRetry")
  public void getPayment(Order order) throws Exception {

    try {
      paymentRequest paymentReq = paymentRequest.newBuilder()
              .setOrderId(order.getId())
              .setAmount(order.getTotalAmount())
              .setCurrency(order.getCurrency())
              .setCustomerEmail(order.getCustomerEmail())
              .setCustomerPhone(order.getCustomerPhone())
              .build();

      paymentResponse paymentResp = this.blockingStubstub.processPayment(paymentReq);

      order.setPaymentId(paymentResp.getPaymentId());
      order.setStatus(OrderStatus.PAYMENT_PROCESSING);
      orderRepository.save(order);

      log.info("Payment initiated: orderId={}, paymentId={}",
              order.getId(), paymentResp.getPaymentId());

    } catch (Exception e) {
    throw new Exception("Payment initiation failed");
    }
  }

  private void paymentServiceCircuitBreakerFallback(Order order,Throwable t) throws Exception {
    try {
      paymentRequest paymentReq = paymentRequest.newBuilder()
              .setOrderId(order.getId())
              .setAmount(order.getTotalAmount())
              .setCurrency(order.getCurrency())
              .setCustomerEmail(order.getCustomerEmail())
              .setCustomerPhone(order.getCustomerPhone())
              .build();

      order.setStatus(OrderStatus.PENDING);
      orderRepository.save(order);
      log.info("Payment pending: orderId={}",
              order.getId());

      this.kafkaProducerService.publishPaymentFailed(paymentReq);
    }
    catch (Exception e) {
      throw new Exception("Pending Payment initiation failed");
    }
  }
  }
