package org.ecomm.paymentservice.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.ecomm.paymentservice.Kafka.KafkaProducerService;
import org.ecomm.paymentservice.Model.Payment;
import org.ecomm.paymentservice.Repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PaymentService {

   private final PaymentRepository paymentRepository;
   private final RazorpayClient razorpayClient;
   private KafkaProducerService kafkaProducerService;

   public PaymentService(PaymentRepository paymentRepository, RazorpayClient razorpayClient, KafkaProducerService kafkaProducerService) {
       this.paymentRepository = paymentRepository;
       this.razorpayClient = razorpayClient;
       this.kafkaProducerService = kafkaProducerService;
   }

   @Transactional
   public Payment createPayment(String orderId,double amount,String currency,String emailaddress,String phoneNumber){
       log.info("Creating payment for order: {}, amount: {}", orderId, amount);
       try {
           JSONObject razorpayOrder = new JSONObject();
           razorpayOrder.put("amount", (int) amount * 100);
           razorpayOrder.put("currency", currency);
           razorpayOrder.put("receipt", orderId);

           Order payorder = razorpayClient.orders.create(razorpayOrder);

           log.info("Razorpay order created: {}", (String)payorder.get("id"));

           Payment paymentCreated= Payment.builder()
                   .orderId(orderId)
                   .amount(amount)
                   .currency(currency)
                   .customerEmail(emailaddress)
                   .customerPhone(phoneNumber)
                   .razorpayOrderId((String)payorder.get("id"))
                   .status(Payment.PaymentStatus.CREATED)
                   .build();

           this.kafkaProducerService.sendPaymentCompletedEvent(paymentCreated);
          return this.paymentRepository.save(paymentCreated);
       }
       catch (RazorpayException exception){
           Payment paymentCreated= Payment.builder()
                   .orderId(orderId)
                   .amount(amount)
                   .currency(currency)
                   .status(Payment.PaymentStatus.FAILED)
                   .build();

           this.kafkaProducerService.sendPaymentFailedEvent(paymentCreated);
           return this.paymentRepository.save(paymentCreated);

       }

   }

    public Payment getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    @Transactional
    public Payment refundPayment(String paymentId, double amount) {
        Payment payment = getPaymentById(paymentId);


        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new RuntimeException("Can only refund successful payments");
        }

        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", (int)(amount * 100));


            razorpayClient.payments.refund(payment.getPaymentId(), refundRequest);
            log.info("Refund initiated for payment: {}", paymentId);

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            return paymentRepository.save(payment);

        } catch (Exception e) {
            log.error("Error processing refund", e);
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }
}
