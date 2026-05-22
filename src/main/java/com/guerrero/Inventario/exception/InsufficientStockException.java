package com.guerrero.Inventario.exception;

/**
 * Stock insuficiente al intentar registrar una venta (HTTP 409).
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
