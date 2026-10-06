package com.ramy.bugreport.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security configuration: which URLs need which role, session-based form login,
 * CORS for the frontend, and password hashing.
 *
 * <p>Authentication is a session cookie created by {@code POST /api/auth/login}. Every URL not listed
 * in the rules is denied ({@code denyAll}). CSRF protection stays at Spring's default (enabled), so
 * state-changing requests need the token from {@code GET /api/csrf}.
 *
 * <p>Authorization has two layers. The URL rules below are the outer gate: they reject requests early,
 * before the controller runs. The services repeat the role checks with {@code @PreAuthorize} and are the
 * authoritative layer, so they stay safe when called from outside the API. Rules that depend on the
 * individual resource (report ownership, comment author) exist only in the services. When changing a
 * role rule, update both places.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	
	/**
	 * Defines the HTTP security rules.
	 *
	 * <ul>
	 * <li>Public: {@code GET /api/csrf} and {@code POST /api/accounts} (registration). Login and logout are handled by their own filters before these rules apply.</li>
	 * <li>ADMIN: listing accounts, changing roles, creating projects.</li>
	 * <li>ADMIN or DEVELOPER: searching users, updating projects, creating and updating components.</li>
	 * <li>Any signed-in user: everything else that is explicitly listed (all reads, creating, updating and closing reports,
	 *     commenting, deleting comments).</li>
	 * <li>Anything not listed: denied.</li>
	 * </ul>
	 *
	 * <p>Login uses form fields to {@code /api/auth/login} and answers 200 or 401 without redirects; logout answers 200.
	 * Errors use {@link ApiAuthenticationEntryPoint} and {@link ApiAccessDeniedHandler}.
	 *
	 * @param http the builder provided by Spring
	 * @param currentUserAuthenticationFilter refreshes the user on every request
	 * @param apiAuthenticationEntryPoint answers 401
	 * @param apiAccessDeniedHandler answers 403
	 * @throws Exception if the filter chain cannot be built
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CurrentUserAuthenticationFilter currentUserAuthenticationFilter,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiAccessDeniedHandler apiAccessDeniedHandler
    ) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.authorizeHttpRequests(auth -> auth
					.requestMatchers(HttpMethod.GET, "/api/csrf")
					.permitAll()

					// API documentation: the OpenAPI spec (JSON and YAML) and Swagger UI.
					.requestMatchers(HttpMethod.GET,
							"/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/**",
							"/swagger-ui.html", "/swagger-ui/**")
					.permitAll()

					// Anyone may register; only admins can view all accounts.
					.requestMatchers(HttpMethod.POST, "/api/accounts")
					.permitAll()
					

					.requestMatchers(HttpMethod.GET, "/api/accounts")
					.hasRole("ADMIN")
					.requestMatchers(HttpMethod.GET, "/api/accounts/developers")
					.authenticated()
					.requestMatchers(HttpMethod.GET, "/api/accounts/users")
					.hasAnyRole("ADMIN", "DEVELOPER")
					.requestMatchers(HttpMethod.PATCH, "/api/accounts/*/role")
					.hasRole("ADMIN")

					// Only admins create projects; admins and developers update projects and components.
					.requestMatchers(HttpMethod.POST, "/api/projects")
					.hasRole("ADMIN")
					.requestMatchers(HttpMethod.PATCH,
							"/api/projects/*/name",
							"/api/projects/*/description")
					.hasAnyRole("ADMIN", "DEVELOPER")
					.requestMatchers(HttpMethod.POST, "/api/components")
					.hasAnyRole("ADMIN", "DEVELOPER")
					.requestMatchers(HttpMethod.PATCH,
							"/api/components/*/name",
							"/api/components/*/description",
							"/api/components/*/responsibleUserId")
					.hasAnyRole("ADMIN", "DEVELOPER")

					// All signed-in users can create, update, and resolve bug reports.
					.requestMatchers(HttpMethod.POST, "/api/reports")
					.authenticated()
					.requestMatchers(HttpMethod.POST, "/api/reports/*/resolution")
					.authenticated()
					.requestMatchers(HttpMethod.PATCH, "/api/reports/*/*")
					.authenticated()

					// All signed-in users can read data and participate in discussions.
					.requestMatchers(HttpMethod.GET, "/api/**")
					.authenticated()
					.requestMatchers(HttpMethod.POST, "/api/comments")
					.authenticated()

					// Comment removal is an administrative action.
					.requestMatchers(HttpMethod.DELETE, "/api/comments/*")
					.authenticated()

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
					.logoutSuccessHandler((request, response, authentication) -> {
						response.setStatus(HttpServletResponse.SC_OK);
					})
			)
			.exceptionHandling((exception) -> exception
				.authenticationEntryPoint(apiAuthenticationEntryPoint)
				.accessDeniedHandler(apiAccessDeniedHandler)
			);

		http.addFilterAfter(currentUserAuthenticationFilter, SecurityContextHolderFilter.class);
		
		return http.build();
	}
	
	/**
	 * Allows the Vite dev server ({@code http://localhost:5173}) to call the API with cookies.
	 * Other origins are rejected; the origin is hard-coded and would need changing for a deployed frontend.
	 */
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

	/** Authenticates form-login credentials against the application's own accounts. */
	@Bean
	public UserDetailsService userDetailsService(IUserAccountRepository userRepository) {
		// Authenticate form-login credentials against application accounts.
		return new UserAccountDetailsService(userRepository);
	}
	
	/** BCrypt encoder used to hash passwords on registration and to check them at login. */
	@Bean
	public PasswordEncoder passwordEncoder() {
		// Store passwords as BCrypt hashes rather than plain text.
		return new BCryptPasswordEncoder();
	}

}
