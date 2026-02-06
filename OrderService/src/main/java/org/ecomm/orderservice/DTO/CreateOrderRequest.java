package org.ecomm.orderservice.DTO;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ecomm.orderservice.Entity.OrderItem;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateOrderRequest {

    public String customerEmail;

    public String customerPhone;

    public Double totalAmount;

    public String currency;

    public List<OrderItem> items;

}
