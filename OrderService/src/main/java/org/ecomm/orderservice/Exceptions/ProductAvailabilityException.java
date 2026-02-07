package org.ecomm.orderservice.Exceptions;

public class ProductAvailabilityException extends RuntimeException {
    public ProductAvailabilityException(String message)
    {
        super(message);
    }
    public ProductAvailabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
