package com.bernardoavila.slotly.resource;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(Long id) {
        super("Resource not found: " + id);
    }
}