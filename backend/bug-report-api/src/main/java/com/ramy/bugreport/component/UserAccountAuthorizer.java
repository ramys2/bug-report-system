package com.ramy.bugreport.component;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.ramy.bugreport.security.UserAccountDetails;

/**
 * Authorization helper used in the {@code @PreAuthorize} expression of {@code UserAccountService.updateRole}
 * (as the bean {@code userAccountAuthorizer}).
 */
@Component
public class UserAccountAuthorizer {

	/**
	 * Prevents users from changing their own role.
	 *
	 * @param userId id of the account whose role would change
	 * @param auth the current authentication
	 * @return {@code true} if the caller is authenticated as a {@link com.ramy.bugreport.security.UserAccountDetails}
	 *         and is a different account than {@code userId}; {@code false} otherwise
	 */
	public boolean canUpdateRole(UUID userId, Authentication auth) {
		if (auth == null || !auth.isAuthenticated()
				|| !(auth.getPrincipal() instanceof UserAccountDetails account)) {
			return false;
		}

		return !account.getId().equals(userId);
	}

}
