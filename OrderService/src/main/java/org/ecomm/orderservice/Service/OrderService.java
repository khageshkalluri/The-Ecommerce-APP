package org.ecomm.orderservice.Service;


import com.ecomm.grpc.payment.*;
import lombok.extern.slf4j.Slf4j;
import org.ecomm.orderservice.DTO.CreateOrderRequest;
import org.ecomm.orderservice.Entity.Order;
import org.ecomm.orderservice.Entity.OrderItem;
import org.ecomm.orderservice.Entity.OrderStatus;
import org.ecomm.orderservice.GrpcClientService.GrpcClientService;
import org.ecomm.orderservice.GrpcClientService.GrpcProductClient;
import org.ecomm.orderservice.Producer.KafkaProducerService;
import org.ecomm.orderservice.Repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.RoundingMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducer;
    private final GrpcClientService grpcClientService;
    private final GrpcProductClient grpcProductClient;

    public OrderService(OrderRepository orderRepository, KafkaProducerService kafkaProducer, GrpcClientService grpcClientService, GrpcProductClient grpcProductClient) {
        this.orderRepository = orderRepository;
        this.kafkaProducer = kafkaProducer;
        this.grpcClientService = grpcClientService;
        this.grpcProductClient = grpcProductClient;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getCustomerEmail());


        if(!this.grpcProductClient.checkProductInventory(request)){
            return Order.builder().build();
        }

        Order order = Order.builder()
                .userId(request.getCustomerEmail())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .totalAmount(BigDecimal.valueOf(request.getItems().stream().map(OrderItem::getPrice).reduce(0.0, Double::sum)).setScale(2, RoundingMode.HALF_UP).doubleValue())
                .currency(request.getCurrency())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order finalOrder = order;
        finalOrder.setItems(request.getItems().stream().map(item -> {
            item.setOrder(finalOrder);
            return item;
        }).toList());

        order = orderRepository.save(finalOrder);


        OrderEvent event = OrderEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType("ORDER_CREATED")
                .setOrderId(order.getId())
                .setUserId(order.getUserId())
                .setPayload(OrderEventPayload.newBuilder()
                        .setOrderId(order.getId())
                        .setCustomerEmail(order.getCustomerEmail())
                        .setCustomerPhone(order.getCustomerPhone())
                        .setTotalAmount(order.getTotalAmount())
                        .setCurrency(order.getCurrency())
                        .setStatus(order.getStatus().toString())
                        .setPaymentId(UUID.randomUUID().toString())
                        .addAllItems(convertToEventItems(order.getItems()))
                        .build())
                .build();

        kafkaProducer.publishOrderCreated(event);

        try {
            this.grpcClientService.getPayment(order);

        } catch (Exception e) {
            log.error("Payment initiation failed: orderId={}", order.getId(), e);
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            kafkaProducer.publishOrderFailed(
                    createFailedEvent(order, "Payment initiation failed")
            );
        }

        return order;
    }

    @Transactional
    public void confirmOrder(String orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentId(paymentId);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);


        OrderEvent event = OrderEvent.newBuilder()
                .setEventType("ORDER_CONFIRMED")
                .setOrderId(order.getId())
                .setUserId(order.getUserId())
                .setPayload(OrderEventPayload.newBuilder()
                        .setOrderId(order.getId())
                        .setUserId(order.getUserId())
                        .setCustomerEmail(order.getCustomerEmail())
                        .setPaymentId(paymentId)
                        .setTotalAmount(order.getTotalAmount())
                        .setStatus(OrderStatus.CONFIRMED.name())
                        .build())
                .build();

        this.grpcProductClient.updateProductInventory(order);
        kafkaProducer.publishOrderConfirmed(event);

        log.info("Order confirmed: orderId={}, paymentId={}", orderId, paymentId);
    }

    @Transactional
    public void failOrder(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        kafkaProducer.publishOrderFailed(createFailedEvent(order, reason));

        log.info("Order failed: orderId={}, reason={}", orderId, reason);
    }

    private OrderEvent createFailedEvent(Order order, String reason) {
        return OrderEvent.newBuilder()
                .setEventType("ORDER_FAILED")
                .setOrderId(order.getId())
                .setUserId(order.getUserId())
                .setPayload(OrderEventPayload.newBuilder()
                        .setOrderId(order.getId())
                        .setUserId(order.getUserId())
                        .setStatus(OrderStatus.FAILED.name())
                        .build())
                .build();
    }

    private List<com.ecomm.grpc.payment.OrderItem> convertToEventItems(
            List<org.ecomm.orderservice.Entity.OrderItem> items) {
        return items.stream()
                .map(item -> com.ecomm.grpc.payment.OrderItem.newBuilder()
                        .setProductId(item.getProductId())
                        .setProductName(item.getProductName())
                        .setQuantity(item.getQuantity())
                        .setPrice(item.getPrice())
                        .build())
                .toList();
    }

    public Order findOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}