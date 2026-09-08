package com.ramy.bugreport.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;

public class UserAccountDetails implements UserDetails {
	
	private static final long serialVersionUID = -3530605535163745849L;
	private final UserAccount userAccount;
	
	public UserAccountDetails(UserAccount userAccount) {
		this.userAccount = userAccount;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(
				new SimpleGrantedAuthority(
						"ROLE_" + this.userAccount.getRole().name()
				)
		);
	}

	@Override
	public @Nullable String getPassword() {
		return this.userAccount.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return this.userAccount.getEmailAddress();
	}
	
	public UUID getId() {
		return this.userAccount.getId();
	}

	public String getName() {
		return this.userAccount.getName();
	}

	public EUserRole getRole() {
		return this.userAccount.getRole();
	}

}
