package com.ramy.bugreport.repository;

import java.util.List;

/**
 * One page of a result list.
 *
 * @param items the items on this page
 * @param totalElements number of items on all pages together
 * @param <T> type of the items
 */
public record PageResult<T>(List<T> items, long totalElements) {
}
