package org.ecomm.productservice.Model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull(message = "name cannot by null")
    @NotBlank(message = "name cannot by blank")
    @NotEmpty(message = "name cannot by empty")
    private String name;

    @NotNull(message = "name cannot by null")
    @NotBlank(message = "name cannot by blank")
    @NotEmpty(message = "name cannot by empty")
    private String description;

    @NotNull
    private double price;

    @NotNull
    private  Long quantity;
}
