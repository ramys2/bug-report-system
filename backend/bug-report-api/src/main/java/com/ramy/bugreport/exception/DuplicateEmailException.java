package com.ramy.bugreport.exception;

public class DuplicateEmailException extends RuntimeException {

    private static final long serialVersionUID = 6046659931344263352L;

	public DuplicateEmailException() {
        super("An account with this email address already exists.");
    }
}
