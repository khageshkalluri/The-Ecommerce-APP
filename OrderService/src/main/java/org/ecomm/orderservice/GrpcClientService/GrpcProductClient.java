package org.ecomm.orderservice.GrpcClientService;

import com.ecomm.grpc.payment.ProductServiceGrpc;
import com.ecomm.grpc.payment.productRequest;
import com.ecomm.grpc.payment.productResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.ecomm.orderservice.DTO.CreateOrderRequest;
import org.ecomm.orderservice.Entity.Order;
import org.ecomm.orderservice.Exceptions.ProductAvailabilityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GrpcProductClient {

    public ProductServiceGrpc.ProductServiceBlockingStub stub;

    public GrpcProductClient(@Value("${product.grpc.server:localhost}")String grpcServer, @Value("${product.grpc.port:9002}") int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcServer, port).usePlaintext().build();
        this.stub = ProductServiceGrpc.newBlockingStub(channel);
    }

    public boolean checkProductInventory(CreateOrderRequest createOrderRequest) {
        try {
            createOrderRequest.getItems().stream().forEach(item -> {
                productRequest request = productRequest.newBuilder()
                        .setProductId(item.getProductId())
                        .setQuantity(item.getQuantity())
                        .setPrice(item.getPrice())
                        .build();
                productResponse productResponse = this.stub.productInventory(request);
                if (!productResponse.getAvailability()) {
                    String message = String.format("%s (ID %s) has availability issues.Please reduce quantity and  try again",
                            item.getProductName(), item.getProductId());
                    throw new ProductAvailabilityException(message);
                }
            });
            return true;
        }
        catch (Exception e) {
            throw new ProductAvailabilityException(e.getMessage(), e);
        }
    }



    public void updateProductInventory(Order orderRequest) {
        try {
            orderRequest.getItems().stream().forEach(item -> {
                productRequest request = productRequest.newBuilder()
                        .setProductId(item.getProductId())
                        .setQuantity((-1*item.getQuantity()))
                        .setPrice(item.getPrice())
                        .build();
                productResponse productResponse = this.stub.productInventory(request);
                if (!productResponse.getAvailability()) {
                    String message = String.format("%s is of ID %s having updating inventory issue. please try again", item.getProductName(), item.getProductId());
                    throw new ProductAvailabilityException(message);
                }
                log.info("Product inventory successfully updated");
            });

        }
        catch (Exception e) {
            throw new ProductAvailabilityException(e.getMessage());
        }
    }

}
