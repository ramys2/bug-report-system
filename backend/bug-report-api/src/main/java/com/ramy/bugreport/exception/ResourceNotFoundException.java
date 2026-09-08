package com.ramy.bugreport.exception;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 16777754540978053L;

	public ResourceNotFoundException(String message) {
        super(message);
    }

}
