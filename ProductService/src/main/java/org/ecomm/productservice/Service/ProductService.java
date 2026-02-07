package org.ecomm.productservice.Service;


import lombok.extern.slf4j.Slf4j;
import org.ecomm.productservice.DTO.PaginatedProductResponseDTO;
import org.ecomm.productservice.DTO.ProductRequestDTO;
import org.ecomm.productservice.DTO.ProductResponseDTO;
import org.ecomm.productservice.Exceptions.ProductNotFoundException;
import org.ecomm.productservice.Mappers.Mappers;
import org.ecomm.productservice.Model.Product;
import org.ecomm.productservice.Repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ProductService {

    ProductRepository productRepository;

    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }


    public PaginatedProductResponseDTO getAllProducts(int page, int size, String sort, String sortField, String searchValue){
        Pageable pageable = PageRequest.of(page-1, size, sort.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending(): Sort.by(sortField).descending());

        Page<Product> productPage ;
        if(searchValue == null || searchValue.isEmpty()){
            productPage = this.productRepository.findAll(pageable);
        }
        else{
            productPage=this.productRepository.findAllByNameEqualsIgnoreCase(searchValue,pageable);
        }

        List<ProductResponseDTO> responseDTOS = productPage.getContent().stream().map(Mappers::EntityToDtoMapping).toList();

       return PaginatedProductResponseDTO.builder()
                .products(responseDTOS)
                .page(productPage.getNumber()+1)
                .size(productPage.getSize())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .build();
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

    public Product checkProductInventory(UUID id){
        Product product1 = this.productRepository.findById(id).orElseThrow(
                ()-> new ProductNotFoundException("Product not found"));
        return product1;
    }

    public Product updateProductInventory(UUID id,long quantity){
        Product product1=this.productRepository.findById(id).orElseThrow(()-> new ProductNotFoundException("Product not found"));
        product1.setQuantity(product1.getQuantity()+quantity);
       return this.productRepository.save(product1);
    }
}
