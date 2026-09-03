package com.ramy.bugreport.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Locale;

import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.repository.IUserAccountRepository;

public class UserAccountDetailsService implements UserDetailsService {
	private final IUserAccountRepository userRepository;
	
	public UserAccountDetailsService(IUserAccountRepository userRepository) {
		this.userRepository = userRepository;
	}
	

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserAccount account = this.userRepository.findByEmailAddress(normalizeEmail(username))
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		
		return new UserAccountDetails(account);
	}

    private String normalizeEmail(String emailAddress) {
        return emailAddress.trim().toLowerCase(Locale.ROOT);
    }

}
