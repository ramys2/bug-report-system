package com.ramy.bugreport.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Re-loads the signed-in user from the database on every request, so that changes such as a new role
 * apply immediately instead of only after the next login.
 *
 * <p>If the account no longer exists, the security context is cleared and the request continues as anonymous.
 */
@Component
public class CurrentUserAuthenticationFilter extends OncePerRequestFilter {

    private final IUserAccountRepository userAccountRepository;

    public CurrentUserAuthenticationFilter(IUserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Replaces the authentication in the security context with one built from the current database state
     * (same credentials and details, fresh {@link UserAccountDetails}). Does nothing for anonymous requests.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserAccountDetails currentUser) {
            userAccountRepository.findById(currentUser.getId()).ifPresentOrElse(account -> {
                var refreshedUser = new UserAccountDetails(account);
                var refreshedAuthentication = new UsernamePasswordAuthenticationToken(
                        refreshedUser, authentication.getCredentials(), refreshedUser.getAuthorities());
                refreshedAuthentication.setDetails(authentication.getDetails());
                SecurityContextHolder.getContext().setAuthentication(refreshedAuthentication);
            }, SecurityContextHolder::clearContext);
        }
        filterChain.doFilter(request, response);
    }
}
