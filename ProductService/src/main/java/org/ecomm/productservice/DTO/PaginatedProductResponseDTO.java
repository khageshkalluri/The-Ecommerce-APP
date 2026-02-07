package org.ecomm.productservice.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginatedProductResponseDTO {

    List<ProductResponseDTO>  products;
    Integer page;
    Integer size;
    Integer totalPages;
    Long totalElements;
}
