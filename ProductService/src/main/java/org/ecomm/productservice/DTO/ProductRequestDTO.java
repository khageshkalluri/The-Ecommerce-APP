package org.ecomm.productservice.DTO;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ecomm.productservice.ValidationGroups.OnCreate;

@Data
public class ProductRequestDTO {

    @NotNull(message = "name cannot by null")
    @NotBlank(message = "name cannot by blank")
    @NotEmpty(message = "name cannot by empty")
     public String name;

    @NotNull(message = "name cannot by null")
    @NotBlank(message = "name cannot by blank")
    @NotEmpty(message = "name cannot by empty")
    public String description;

    @NotNull
     public Double price;

    @NotNull
    @Min(value = 1,groups = OnCreate.class)
      public Long quantity;
}
