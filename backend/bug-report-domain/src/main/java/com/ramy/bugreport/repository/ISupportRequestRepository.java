package com.ramy.bugreport.repository;

import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.SupportRequest;

/** Storage contract for support requests, implemented by the API module's JPA adapter. */
public interface ISupportRequestRepository {
    Optional<SupportRequest> findById(UUID id);

    /** Saves the request, returning its generated id for a new request. */
    SupportRequest save(SupportRequest request);

    /** Whether the user already has a pending role-change request for this role. */
    boolean existsPendingRoleChange(UUID requesterId, EUserRole requestedRole);

    long count();

    /** Removes requests, mainly for test setup; call before deleting their requester accounts. */
    void deleteAll();
}
