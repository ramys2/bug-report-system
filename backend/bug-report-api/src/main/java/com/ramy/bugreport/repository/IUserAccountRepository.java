package com.ramy.bugreport.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.UserAccount;

public interface IUserAccountRepository extends JpaRepository<UserAccount, UUID> {
    // No further implementation needed for now
}
