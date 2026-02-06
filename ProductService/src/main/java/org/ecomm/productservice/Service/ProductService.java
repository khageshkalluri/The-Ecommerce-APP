package org.ecomm.productservice.Service;


import org.ecomm.productservice.DTO.ProductRequestDTO;
import org.ecomm.productservice.DTO.ProductResponseDTO;
import org.ecomm.productservice.Exceptions.ProductNotFoundException;
import org.ecomm.productservice.Mappers.Mappers;
import org.ecomm.productservice.Model.Product;
import org.ecomm.productservice.Repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    ProductRepository productRepository;

    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }


    public List<ProductResponseDTO> getAllProducts(){
        List<Product> products= this.productRepository.findAll();
        return products.stream().map(Mappers::EntityToDtoMapping).toList();
    }

    public ProductResponseDTO getProductById(UUID id){
        Product product = this.productRepository.findById(id).orElseThrow(
                ()-> new ProductNotFoundException("Product not found"));

        return Mappers.EntityToDtoMapping(product);
    }

    public List<ProductResponseDTO> getProductsByName(String name){
      List<Product> products=this.productRepository.getProductsByName(name);

      return products.stream()
              .map(Mappers::EntityToDtoMapping)
              .toList();
    }

    public ProductResponseDTO addProduct(ProductRequestDTO product){
        Product product1= Mappers.DtoToEntityMapping(product);
        return Mappers.EntityToDtoMapping(productRepository.save(product1));
    }

    public ProductResponseDTO updateProduct(UUID id, ProductRequestDTO product){
        Product product1 = this.productRepository.findById(id).orElseThrow(
                ()-> new ProductNotFoundException("Product not found"));

        product1.setName(product.getName());
        product1.setDescription(product.getDescription());
        product1.setPrice(product.getPrice());
        product1.setQuantity(product.getQuantity());

        return Mappers.EntityToDtoMapping(productRepository.save(product1));
    }

    public void deleteProduct(UUID id){
        Product product1 = this.productRepository.findById(id).orElseThrow(
                ()-> new ProductNotFoundException("Product not found"));

        productRepository.deleteById(product1.getId());
    }

}
