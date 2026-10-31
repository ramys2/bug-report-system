package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.account.UpdateRoleRequest;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ErrorCode;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.security.UserAccountDetails;

import jakarta.validation.Validator;

/** Real MariaDB constraints and transactions; no test-wide transaction hides commit failures. */
@SpringBootTest(properties = "spring.jms.listener.auto-startup=false")
@ActiveProfiles("test")
class GoogleAccountServiceIT {
    @Autowired private GoogleAccountService google;
    @Autowired private UserAccountService accounts;
    @Autowired private IUserAccountRepository users;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private Validator validator;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        for (String table : List.of("support_request", "comment", "bug_report", "component", "software_project",
                "resolution", "user_account")) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    @Test
    void firstLoginCreatesPasswordlessReporterWithUniqueGeneratedName() {
        UserAccount first = google.resolve("subject-A", " First@Example.COM ");
        UserAccount second = google.resolve("subject-B", "second@example.com");
        assertThat(first.getRole()).isEqualTo(EUserRole.REPORTER);
        assertThat(first.getPasswordHash()).isNull();
        assertThat(first.getEmailAddress()).isEqualTo("first@example.com");
        assertThat(first.getName()).startsWith("google-").hasSize(43).isNotEqualTo(second.getName());
        var stored = users.findById(first.getId()).orElseThrow();
        assertThat(stored.getGoogleSubject()).isEqualTo("subject-A");
        assertThat(stored.getPasswordHash()).isNull();
    }

    @Test
    void returningSubjectUpdatesEmailAndPreservesIdentityNamePasswordAndRole() {
        var original = users.save(new UserAccount(null, "preserved-name", "old@example.com", "established-hash",
                EUserRole.ADMIN, "subject"));
        var updated = google.resolve("subject", "new@example.com");
        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getEmailAddress()).isEqualTo("new@example.com");
        assertThat(updated.getName()).isEqualTo(original.getName());
        assertThat(updated.getRole()).isEqualTo(EUserRole.ADMIN);
        assertThat(updated.getPasswordHash()).isEqualTo("established-hash");
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void newSubjectCannotClaimLocalEmailOrAnotherGoogleAccountsEmail() {
        users.save(new UserAccount("local", "local@example.com", "hash", EUserRole.ADMIN));
        google.resolve("original-subject", "google@example.com");
        assertConflict(() -> google.resolve("new-subject", "LOCAL@EXAMPLE.COM"));
        assertConflict(() -> google.resolve("new-subject", "google@example.com"));
        assertThat(users.count()).isEqualTo(2);
        assertThat(users.findByGoogleSubject("new-subject")).isEmpty();
    }

    @Test
    void returningEmailConflictRollsBackWithoutChangingEitherAccount() {
        var original = google.resolve("subject", "original@example.com");
        users.updateRole(original.getId(), EUserRole.DEVELOPER);
        var other = users.save(new UserAccount("local", "taken@example.com", "hash", EUserRole.ADMIN));
        assertConflict(() -> google.resolve("subject", "taken@example.com"));
        var unchanged = users.findById(original.getId()).orElseThrow();
        assertThat(unchanged.getEmailAddress()).isEqualTo("original@example.com");
        assertThat(unchanged.getName()).isEqualTo(original.getName());
        assertThat(unchanged.getRole()).isEqualTo(EUserRole.DEVELOPER);
        assertThat(users.findById(other.getId()).orElseThrow().getGoogleSubject()).isNull();
        assertThat(users.count()).isEqualTo(2);
    }

    @Test
    void subjectComparisonIsCaseSensitiveAndNeverUsesEmailAsIdentity() {
        var upper = google.resolve("Subject", "upper@example.com");
        var lower = google.resolve("subject", "lower@example.com");
        assertThat(upper.getId()).isNotEqualTo(lower.getId());
        assertThat(google.resolve("Subject", "changed@example.com").getId()).isEqualTo(upper.getId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-an-email", " ", "a@", "a\r\n@example.com"})
    void invalidEmailIsRejectedWithoutWrites(String email) {
        assertThatThrownBy(() -> google.resolve("subject", email)).isInstanceOf(BusinessRuleConflictException.class);
        assertThat(users.count()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " subject", "subject\n", "příliš"})
    void invalidSubjectIsRejectedWithoutNormalizationOrWrites(String subject) {
        assertThatThrownBy(() -> google.resolve(subject, "valid@example.com"))
                .isInstanceOf(BusinessRuleConflictException.class);
        assertThat(users.count()).isZero();
    }

    @Test
    void databaseEnforcesSubjectUniquenessAndAtLeastOneLoginMethod() {
        google.resolve("subject", "first@example.com");
        assertThatThrownBy(() -> users.save(new UserAccount(null, "other", "other@example.com", null,
                EUserRole.REPORTER, "subject"))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> users.save(new UserAccount("no-login", "none@example.com", null, EUserRole.REPORTER)))
                .isInstanceOf(DataIntegrityViolationException.class);
        users.save(new UserAccount("local-1", "local1@example.com", "hash", EUserRole.REPORTER));
        users.save(new UserAccount("local-2", "local2@example.com", "hash", EUserRole.REPORTER));
        assertThat(users.count()).isEqualTo(3);
    }

    @Test
    void generatedUsernameCollisionIsRetriedAndRetriesAreBounded() {
        users.save(new UserAccount("occupied", "local@example.com", "hash", EUserRole.REPORTER));
        AtomicInteger attempts = new AtomicInteger();
        var collisionThenSuccess = new GoogleAccountService(users, validator, transactions) {
            @Override String generateUsername() {
                return attempts.getAndIncrement() == 0 ? "occupied" : "google-" + UUID.randomUUID();
            }
        };
        assertThat(collisionThenSuccess.resolve("subject", "google@example.com").getName()).startsWith("google-");
        attempts.set(0);
        var alwaysCollides = new GoogleAccountService(users, validator, transactions) {
            @Override String generateUsername() {
                attempts.incrementAndGet();
                return "occupied";
            }
        };
        assertThatThrownBy(() -> alwaysCollides.resolve("another", "another@example.com"))
                .isInstanceOf(BusinessRuleConflictException.class);
        assertThat(attempts).hasValue(3);
        assertThat(users.count()).isEqualTo(2);
    }

    @Test
    void concurrentFirstLoginsResolveToOneLocalAccount() throws Exception {
        var result = race(() -> google.resolve("same-subject", "same@example.com").getId(),
                () -> google.resolve("same-subject", "same@example.com").getId());
        assertThat(result.get(0)).isEqualTo(result.get(1));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void differentSubjectsRacingForOneEmailNeverMerge() throws Exception {
        var result = race(() -> resolveOrConflict("subject-1", "same@example.com"),
                () -> resolveOrConflict("subject-2", "same@example.com"));
        assertThat(result).containsExactlyInAnyOrder("success", "conflict");
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void concurrentEmailUpdatesRespectUniquenessAndRollbackTheLoser() throws Exception {
        var first = google.resolve("subject-1", "first@example.com");
        var second = google.resolve("subject-2", "second@example.com");
        var result = race(() -> resolveOrConflict("subject-1", "shared@example.com"),
                () -> resolveOrConflict("subject-2", "shared@example.com"));
        assertThat(result).containsExactlyInAnyOrder("success", "conflict");
        var emails = users.findAll().stream().map(UserAccount::getEmailAddress).toList();
        assertThat(emails).contains("shared@example.com");
        assertThat(emails).containsAnyOf(first.getEmailAddress(), second.getEmailAddress());
        assertThat(users.count()).isEqualTo(2);
    }

    @Test
    void concurrentRoleChangeAndGoogleEmailUpdatePreserveBothChanges() throws Exception {
        var target = google.resolve("subject", "old@example.com");
        var admin = users.save(new UserAccount("admin", "admin@example.com", "hash", EUserRole.ADMIN));
        race(() -> google.resolve("subject", "new@example.com").getId(), () -> {
            var principal = new UserAccountDetails(admin);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()));
            try {
                accounts.updateRole(target.getId(), new UpdateRoleRequest(EUserRole.DEVELOPER));
                return target.getId();
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
        var updated = users.findById(target.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(EUserRole.DEVELOPER);
        assertThat(updated.getEmailAddress()).isEqualTo("new@example.com");
        assertThat(updated.getGoogleSubject()).isEqualTo("subject");
    }

    private String resolveOrConflict(String subject, String email) {
        try {
            google.resolve(subject, email);
            return "success";
        } catch (BusinessRuleConflictException exception) {
            assertThat(exception.getCode()).isEqualTo(ErrorCode.GOOGLE_EMAIL_CONFLICT);
            return "conflict";
        }
    }

    private void assertConflict(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessRuleConflictException.class,
                exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.GOOGLE_EMAIL_CONFLICT));
    }

    private <T> List<T> race(Callable<T> first, Callable<T> second) throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> { start.await(); return first.call(); });
            var two = executor.submit(() -> { start.await(); return second.call(); });
            start.countDown();
            return List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        }
    }
}
