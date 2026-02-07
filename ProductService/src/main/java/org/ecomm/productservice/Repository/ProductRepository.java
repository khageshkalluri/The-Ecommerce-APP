package org.ecomm.productservice.Repository;

import org.ecomm.productservice.DTO.ProductResponseDTO;
import org.ecomm.productservice.Model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query(value = "Select * from products where name LIKE %:name%", nativeQuery = true)
    List<Product> getProductsByName(@Param("name") String name);
    Page<Product> findAllByNameEqualsIgnoreCase(String name, Pageable pageable);
}
