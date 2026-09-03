package com.ramy.bugreport.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;

public interface IUserAccountRepository extends JpaRepository<UserAccount, UUID> {
    // No further implementation needed for now
	
	Optional<UserAccount> findByEmailAddress(String emailAddress);

    boolean existsByEmailAddress(String emailAddress);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.List<UserAccount> findAllByRole(EUserRole role);
}
