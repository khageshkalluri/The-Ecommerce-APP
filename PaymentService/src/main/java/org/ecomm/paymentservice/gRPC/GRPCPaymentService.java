package org.ecomm.paymentservice.gRPC;

import com.ecomm.grpc.payment.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.ecomm.paymentservice.Kafka.KafkaProducerService;
import org.ecomm.paymentservice.Model.Payment;
import org.ecomm.paymentservice.Service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

@GrpcService
public class GRPCPaymentService extends PaymentServiceGrpc.PaymentServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GRPCPaymentService.class);

    private final PaymentService paymentService;
    private final KafkaProducerService kafkaProducerService;

    @Value("${razorpay.enabled}")
    private boolean paymentEnabled;

    public GRPCPaymentService(PaymentService paymentService,KafkaProducerService kafkaProducerService) {
        this.paymentService = paymentService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public void processPayment(paymentRequest request, StreamObserver<paymentResponse> observer) {

        log.info("Received payment request for order: {}", request.getOrderId());
          if(paymentEnabled) {
              try {
                  Payment payment = paymentService.createPayment(
                          request.getOrderId(),
                          request.getAmount(),
                          request.getCurrency(),
                          request.getCustomerEmail(),
                          request.getCustomerPhone()
                  );

                  paymentResponse response = paymentResponse.newBuilder()
                          .setPaymentId(payment.getPaymentId())
                          .setAmount(payment.getAmount())
                          .setStatus(payment.getStatus().name())
                          .setRazorPayOrderId(payment.getRazorpayOrderId() != null ?
                                  payment.getRazorpayOrderId() : "")
                          .setMessage(payment.getStatus() == Payment.PaymentStatus.CREATED ?
                                  "Payment order created successfully" :
                                  "Payment creation failed")
                          .build();

                  observer.onNext(response);
                  observer.onCompleted();

                  log.info("Payment response sent: {}", payment.getPaymentId());
                  this.kafkaProducerService.sendPaymentCompletedEvent(payment);


              } catch (Exception e) {
                  log.error("Error processing payment", e);
                  observer.onError(e);
              }
          }
          else{
              paymentResponse response = paymentResponse.newBuilder()
                      .setPaymentId("1")
                      .setAmount(100)
                      .setStatus(Payment.PaymentStatus.SUCCESS.name())
                      .setRazorPayOrderId("1")
                      .setMessage(" Sample Payment order created successfully ")
                      .build();


              Payment payment = Payment.builder()
                      .paymentId(UUID.randomUUID().toString())
                      .amount(request.getAmount())
                      .status(Payment.PaymentStatus.SUCCESS)
                      .orderId(request.getOrderId())
                      .build();

              observer.onNext(response);
              observer.onCompleted();

              log.info("Payment response sent");

              this.kafkaProducerService.sendPaymentCompletedEvent(payment);
          }
    }
    @Override
    public void getPaymentStatus(paymentStatusRequest request,
                                 StreamObserver<paymentStatusResponse> responseObserver) {
        if(paymentEnabled) {
            try {
                Payment payment = paymentService.getPaymentById(request.getPaymentId());

                paymentStatusResponse response = paymentStatusResponse.newBuilder()
                        .setPaymentId(payment.getPaymentId())
                        .setStatus(payment.getStatus().name())
                        .setAmount(payment.getAmount())
                        .setOrderId(payment.getOrderId())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();

            } catch (Exception e) {
                log.error("Error getting payment status", e);
                responseObserver.onError(e);
            }
        }
             else{
                paymentStatusResponse response = paymentStatusResponse.newBuilder()
                        .setPaymentId("1")
                        .setAmount(100)
                        .setStatus(Payment.PaymentStatus.SUCCESS.name())
                        .setOrderId("1")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();

                log.info(" Sample Payment status sent");
            }
    }

    @Override
    public void refundPayment(refundRequest request,
                              StreamObserver<refundResponse> responseObserver) {
        if (paymentEnabled) {
            try {
                Payment payment = paymentService.refundPayment(
                        request.getPaymentId(),
                        request.getAmount()
                );

                refundResponse response = refundResponse.newBuilder()
                        .setRefundId("REF-" + payment.getPaymentId())
                        .setSuccess(true)
                        .setMessage("Refund processed successfully")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();

            } catch (Exception e) {
                log.error("Error processing refund", e);

                refundResponse response = refundResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Refund failed: " + e.getMessage())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        else{
            refundResponse response = refundResponse.newBuilder()
                    .setRefundId("REF-101")
                    .setSuccess(true)
                    .setMessage(" Sample Refund processed successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info(" Sample Refund initiated successfully");
        }
    }
    }