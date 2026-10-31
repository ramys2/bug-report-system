package com.ramy.bugreport.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ErrorCode;
import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Resolves Google identities without ever linking accounts by email. */
@Service
public class GoogleAccountService {
    private final IUserAccountRepository users;
    private final Validator validator;
    private final TransactionTemplate transaction;

    public GoogleAccountService(IUserAccountRepository users, Validator validator,
            PlatformTransactionManager transactionManager) {
        this.users = users;
        this.validator = validator;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Called only with an email verified by the OIDC user service. */
    public UserAccount resolve(String subject, String verifiedEmail) {
        String email = verifiedEmail == null ? null : verifiedEmail.trim().toLowerCase(Locale.ROOT);
        if (!validator.validate(new Profile(subject, email)).isEmpty()) {
            throw new BusinessRuleConflictException(ErrorCode.GOOGLE_LOGIN_FAILED, "Invalid Google account profile.");
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            String name = generateUsername();
            // Read the identity before the write transaction, then lock its row as that transaction's
            // first read. MariaDB snapshot isolation otherwise rejects concurrent role/email changes.
            var existing = users.findByGoogleSubject(subject);
            try {
                return transaction.execute(status -> resolveInTransaction(subject, email, name, existing));
            } catch (DataIntegrityViolationException exception) {
                // execute() has already rolled back. These reads must not reuse its failed transaction.
                var concurrent = users.findByGoogleSubject(subject);
                if (concurrent.isPresent()) {
                    // A concurrent registration may have won, or an existing user's new email conflicted.
                    // Retry resolution once in a fresh transaction, where conflicts still fail closed.
                    try {
                        return transaction.execute(status -> resolveInTransaction(subject, email, name, concurrent));
                    } catch (DataIntegrityViolationException conflict) {
                        if (emailBelongsToAnotherAccount(subject, email)) {
                            throw emailConflict();
                        }
                        throw conflict;
                    }
                }
                if (users.existsByEmailAddress(email)) {
                    throw emailConflict();
                }
                if (!users.existsByName(name)) {
                    throw exception;
                }
                // Only a generated username collision is retryable with another username.
            }
        }
        throw new BusinessRuleConflictException(ErrorCode.GOOGLE_LOGIN_FAILED, "Could not create Google account.");
    }

    private UserAccount resolveInTransaction(String subject, String email, String name, Optional<UserAccount> existing) {
        if (existing.isEmpty()) {
            if (users.existsByEmailAddress(email)) {
                throw emailConflict();
            }
            return users.save(new UserAccount(null, name, email, null, EUserRole.REPORTER, subject));
        }

        // Serialize contact changes for this identity. Never overwrite its role with a stale snapshot.
        UserAccount account = users.findByIdForUpdate(existing.get().getId()).orElseThrow(() ->
                new BusinessRuleConflictException(ErrorCode.GOOGLE_LOGIN_FAILED, "Account no longer exists."));
        if (!email.equals(account.getEmailAddress())) {
            if (emailBelongsToAnotherAccount(subject, email)) {
                throw emailConflict();
            }
            users.updateEmailAddress(account.getId(), email);
            return users.findById(account.getId()).orElseThrow();
        }
        return account;
    }

    private boolean emailBelongsToAnotherAccount(String subject, String email) {
        return users.findByEmailAddress(email)
                .filter(account -> !subject.equals(account.getGoogleSubject())).isPresent();
    }

    private BusinessRuleConflictException emailConflict() {
        return new BusinessRuleConflictException(ErrorCode.GOOGLE_EMAIL_CONFLICT,
                "The Google email cannot be used for this account.");
    }

    String generateUsername() {
        return "google-" + UUID.randomUUID();
    }

    private record Profile(
            @NotBlank @Pattern(regexp = "[\\x21-\\x7e]{1,255}") String subject,
            @NotBlank @Email @Size(max = 255) String email) {
    }
}
