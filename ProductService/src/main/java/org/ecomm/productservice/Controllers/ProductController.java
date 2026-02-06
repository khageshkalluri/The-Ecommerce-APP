package org.ecomm.productservice.Controllers;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.ecomm.productservice.DTO.ProductRequestDTO;
import org.ecomm.productservice.DTO.ProductResponseDTO;
import org.ecomm.productservice.Service.ProductService;
import org.ecomm.productservice.ValidationGroups.OnCreate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable("id") UUID id) {
        ProductResponseDTO  productResponseDTO = this.productService.getProductById(id);
        return new ResponseEntity<>(productResponseDTO, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDTO>> getProductByName(@RequestParam("name") String name) {
        List<ProductResponseDTO> productResponseDTO = this.productService.getProductsByName(name);
       return new ResponseEntity<>(productResponseDTO, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        List<ProductResponseDTO> productResponseDTO= this.productService.getAllProducts();
        return new ResponseEntity<>(productResponseDTO, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<ProductResponseDTO> addProduct(@Validated({Default.class, OnCreate.class}) @RequestBody ProductRequestDTO productRequestDTO) {
        ProductResponseDTO productResponseDTO = this.productService.addProduct(productRequestDTO);
        return new ResponseEntity<>(productResponseDTO, HttpStatus.CREATED);
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable("id") UUID id, @Valid @RequestBody ProductRequestDTO productRequestDTO) {
        ProductResponseDTO productResponseDTO = this.productService.updateProduct(id, productRequestDTO);
        return new ResponseEntity<>(productResponseDTO, HttpStatus.ACCEPTED);
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<ProductResponseDTO> deleteProduct(@PathVariable("id") UUID id) {
        this.productService.deleteProduct(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
