package com.ramy.bugreport.repository;

/**
 * Which page of a result list to read. Framework-free, so the domain module does not depend on Spring Data.
 *
 * @param page zero-based page number, 0 or more
 * @param size number of items per page, 1 or more
 */
public record PageQuery(int page, int size) {

    public PageQuery {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be 0 or more and size must be 1 or more");
        }
    }
}
