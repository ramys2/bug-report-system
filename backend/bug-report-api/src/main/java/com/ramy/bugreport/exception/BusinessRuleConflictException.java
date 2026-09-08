package com.ramy.bugreport.exception;

public class BusinessRuleConflictException extends RuntimeException {

    private static final long serialVersionUID = 2145687505827373609L;

	public BusinessRuleConflictException(String message) {
        super(message);
    }
}
