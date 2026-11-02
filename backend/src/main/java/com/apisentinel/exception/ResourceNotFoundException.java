package com.apisentinel.exception;

public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String resource, Long id)
    {
        super(resource + "Nnot found with id " + id);
    }
}
