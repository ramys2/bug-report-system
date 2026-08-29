package com.ramy.bugreport.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.authorizeHttpRequests(auth -> auth
					.requestMatchers(HttpMethod.GET, "/api/csrf")
					.permitAll()

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
			.formLogin((form) -> form
					.loginProcessingUrl("/api/auth/login")
					.successHandler((request, response, authentication) -> {
						response.setStatus(HttpServletResponse.SC_OK);
					})
					.failureHandler((request, response, exception) -> {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					})
					.permitAll()
			)
			.logout((logout) -> logout
					.logoutUrl("/api/auth/logout")
			)
			.exceptionHandling((exception) -> exception
					.authenticationEntryPoint(
							new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
					)
			);
		
		return http.build();
	}
	
	@Bean
	UrlBasedCorsConfigurationSource corsConfigurationSource() {
	    CorsConfiguration configuration = new CorsConfiguration();

	    configuration.setAllowedOrigins(
	        List.of("http://localhost:5173")
	    );

	    configuration.setAllowedMethods(
	    	List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS")
	    );
	    
	    configuration.setAllowedHeaders(
	        List.of("Content-Type", "X-CSRF-TOKEN")
	    );

	    configuration.setAllowCredentials(true);

	    UrlBasedCorsConfigurationSource source =
	        new UrlBasedCorsConfigurationSource();

	    source.registerCorsConfiguration("/**", configuration);

	    return source;
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
