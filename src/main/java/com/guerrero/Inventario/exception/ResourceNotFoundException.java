package com.guerrero.Inventario.exception;

/**
 * Recurso solicitado no existe (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entidad, Object id) {
        super(entidad + " no encontrado con id " + id);
    }
}
