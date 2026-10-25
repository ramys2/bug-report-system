package com.ramy.bugreport.messaging.consumer;

/** Marks a safely logged failure while still allowing the JMS transaction to roll back. */
class NotificationProcessingException extends RuntimeException {
    NotificationProcessingException() {
        super("Notification processing failed");
    }
}
