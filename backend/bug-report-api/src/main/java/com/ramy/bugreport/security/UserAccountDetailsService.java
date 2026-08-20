package com.ramy.bugreport.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.repository.IUserAccountRepository;

public class UserAccountDetailsService implements UserDetailsService {
	private final IUserAccountRepository userRepository;
	
	public UserAccountDetailsService(IUserAccountRepository userRepository) {
		this.userRepository = userRepository;
	}
	

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserAccount account = this.userRepository.findByEmailAddress(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		
		return new UserAccountDetails(account);
	}

}
