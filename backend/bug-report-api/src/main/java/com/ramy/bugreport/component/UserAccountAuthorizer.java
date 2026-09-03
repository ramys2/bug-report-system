package com.ramy.bugreport.component;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.ramy.bugreport.security.UserAccountDetails;

@Component
public class UserAccountAuthorizer {

	public boolean canUpdateRole(UUID userId, Authentication auth) {
		if (auth == null || !auth.isAuthenticated()
				|| !(auth.getPrincipal() instanceof UserAccountDetails account)) {
			return false;
		}

		return !account.getId().equals(userId);
	}

}
