package org.ecomm.productservice.Mappers;

import org.ecomm.productservice.DTO.ProductRequestDTO;
import org.ecomm.productservice.DTO.ProductResponseDTO;
import org.ecomm.productservice.Model.Product;


public class Mappers {

    public static Product DtoToEntityMapping(ProductRequestDTO productRequestDTO){
       return Product.builder().name(productRequestDTO.getName())
                .description(productRequestDTO.getDescription())
                .price(productRequestDTO.getPrice())
                .quantity(productRequestDTO.getQuantity())
                .build();
    }

    public static ProductResponseDTO EntityToDtoMapping(Product product){
        return ProductResponseDTO.builder()
                .productId(String.valueOf(product.getId()))
                .name(product.getName())
                .description(product.getDescription())
                .price(Double.toString(product.getPrice()))
                .quantity(Long.toString(product.getQuantity()))
                .build();
    }

}
