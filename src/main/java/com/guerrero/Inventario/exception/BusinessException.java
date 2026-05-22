package com.guerrero.Inventario.exception;

/**
 * Violacion de una regla de negocio (HTTP 400 / 409).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
