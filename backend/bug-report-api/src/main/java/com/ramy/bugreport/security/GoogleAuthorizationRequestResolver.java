package com.ramy.bugreport.security;

import java.net.URI;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import jakarta.servlet.http.HttpServletRequest;

/** Remembers a safe application route; Spring still builds and validates the OAuth request. */
public class GoogleAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    static final String RETURN_TO = GoogleAuthorizationRequestResolver.class.getName() + ".returnTo";
    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public GoogleAuthorizationRequestResolver(ClientRegistrationRepository registrations) {
        delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations,
                GoogleLoginConfiguration.AUTHORIZATION_BASE);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return remember(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        return remember(request, delegate.resolve(request, registrationId));
    }

    private OAuth2AuthorizationRequest remember(HttpServletRequest request, OAuth2AuthorizationRequest authorization) {
        if (authorization != null) {
            request.getSession().setAttribute(RETURN_TO, safeReturnTo(request.getParameter("returnTo")));
        }
        return authorization;
    }

    static String safeReturnTo(String candidate) {
        if (candidate == null || candidate.length() > 2048 || !candidate.startsWith("/")
                || candidate.startsWith("//") || candidate.contains("\\")
                || candidate.chars().anyMatch(character -> Character.isISOControl(character))) {
            return "/";
        }
        try {
            URI uri = URI.create(candidate);
            String path = uri.getPath();
            // Only known application pages can be return targets, never API or authentication endpoints.
            boolean applicationPage = path.equals("/")
                    || path.matches("/reports/[0-9a-fA-F-]{36}")
                    || path.matches("/admin/(users|support|components|projects)");
            if (uri.isAbsolute() || uri.getRawAuthority() != null || !applicationPage
                    || path.contains("\\") || uri.toString().matches("(?i).*%(0[0-9a-f]|1[0-9a-f]|7f).*")) {
                return "/";
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            return "/";
        }
    }
}
