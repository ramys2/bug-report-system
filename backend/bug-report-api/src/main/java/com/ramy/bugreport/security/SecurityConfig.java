package com.ramy.bugreport.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import com.ramy.bugreport.repository.IUserAccountRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// The current SPA client does not send CSRF tokens with its form posts.
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
					// Anyone may register; only admins can view all accounts.
					.requestMatchers(HttpMethod.POST, "/api/accounts")
					.permitAll()
					.requestMatchers(HttpMethod.GET, "/api/accounts")
					.hasRole("ADMIN")

					// Only admins create projects; admins and developers create components.
					.requestMatchers(HttpMethod.POST, "/api/projects")
					.hasRole("ADMIN")
					.requestMatchers(HttpMethod.POST, "/api/components")
					.hasAnyRole("ADMIN", "DEVELOPER")

					// All signed-in users can create, update, and resolve bug reports.
					.requestMatchers(HttpMethod.POST, "/api/reports")
					.authenticated()
					.requestMatchers(HttpMethod.POST, "/api/reports/*/resolution")
					.authenticated()
					.requestMatchers(HttpMethod.PATCH, "/api/reports/*")
					.authenticated()

					// All signed-in users can read data and participate in discussions.
					.requestMatchers(HttpMethod.GET, "/api/**")
					.authenticated()
					.requestMatchers(HttpMethod.POST, "/api/reports/*/comments")
					.authenticated()

					// Comment removal is an administrative action.
					.requestMatchers(HttpMethod.DELETE, "/api/comments/*")
					.hasRole("ADMIN")

					// Reject any endpoint that has not been explicitly allowed above.
					.anyRequest().denyAll()
			)
			.formLogin(form -> form
					// The client submits email/password, not Spring Security's default username/password.
					.usernameParameter("email")
					.passwordParameter("password")
					// The client logs in through Ajax and expects a response rather than a redirect.
					.successHandler((request, response, authentication) -> response.setStatus(200))
					.failureHandler((request, response, exception) -> response.sendError(401))
					.permitAll());
		
		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService(IUserAccountRepository userRepository) {
		// Authenticate form-login credentials against application accounts.
		return new UserAccountDetailsService(userRepository);
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		// Store passwords as BCrypt hashes rather than plain text.
		return new BCryptPasswordEncoder();
	}

}
