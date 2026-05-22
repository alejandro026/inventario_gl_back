package com.guerrero.Inventario.exception;

/**
 * Intento de crear un recurso que ya existe (HTTP 409).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
