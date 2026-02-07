package org.ecomm.productservice.GrpcProductServer;

import com.ecomm.grpc.payment.ProductServiceGrpc;
import com.ecomm.grpc.payment.productRequest;
import com.ecomm.grpc.payment.productResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.ecomm.productservice.Exceptions.ProductNotFoundException;
import org.ecomm.productservice.Model.Product;
import org.ecomm.productservice.Service.ProductService;

import java.util.UUID;

@GrpcService
@Slf4j
public class GrpcProductServer extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductService productService;
    public GrpcProductServer(ProductService productService) {
        this.productService = productService;
    }



    @Override
    public void productInventory(productRequest request,StreamObserver<productResponse> responseObserver) {
        try {
            if (request.getQuantity() <= 0) {
                Product product = this.productService.updateProductInventory(UUID.fromString(request.getProductId()), request.getQuantity());
                productResponse response = productResponse.newBuilder()
                        .setProductId(String.valueOf(product.getId()))
                        .setQuantity(product.getQuantity())
                        .setAvailability(true)
                        .build();

                log.info("Product inventory successfully updated");
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
            else{
            Product product = this.productService.checkProductInventory(UUID.fromString(request.getProductId()));
            productResponse response = productResponse.newBuilder().setProductId(String.valueOf(product.getId()))
                    .setQuantity(product.getQuantity())
                    .setAvailability(product.getQuantity() >= request.getQuantity())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
        catch (ProductNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
        catch(Exception e) {
            responseObserver.onError(e);
        }
    }
}
