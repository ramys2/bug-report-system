package com.ramy.bugreport.domain;

/** Lifecycle of a support request. Stored by name in the database. */
public enum ESupportRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
