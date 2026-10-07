package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.persistence.entity.UserAccountEntity;

import jakarta.persistence.LockModeType;

/** Spring Data repository for {@link com.ramy.bugreport.persistence.entity.UserAccountEntity}. */
public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, UUID> {
    /** Account with exactly this email address (comparison is left to the database collation). */
    Optional<UserAccountEntity> findByEmailAddress(String emailAddress);
    /** Whether an account with exactly this email address exists. */
    boolean existsByEmailAddress(String emailAddress);
    /** Accounts whose name contains the text, ignoring case. */
    List<UserAccountEntity> findByNameContainingIgnoreCase(String name);

    /**
     * One page of the accounts that match all given conditions, ordered by name and then id. A {@code null} parameter
     * means "no restriction". The text parameters are complete LIKE patterns (e.g. {@code %ali%}) in lower case, with
     * {@code !} as the escape character; Spring Data derives the count query for the total from this query.
     */
    @Query("""
            select u from UserAccountEntity u
            where (:id is null or cast(u.id as string) like :id escape '!')
              and (:name is null or lower(u.name) like :name escape '!')
              and (:email is null or lower(u.emailAddress) like :email escape '!')
              and (:role is null or u.role = :role)
            order by u.name, u.id
            """)
    Page<UserAccountEntity> findPage(
            @Param("id") String idPattern,
            @Param("name") String namePattern,
            @Param("email") String emailPattern,
            @Param("role") EUserRole role,
            Pageable pageable);

    /**
     * Accounts with the given role, read with a pessimistic write lock (held until the surrounding transaction ends),
     * which lets callers serialize concurrent changes such as demoting admins.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<UserAccountEntity> findAllByRole(EUserRole role);
}
