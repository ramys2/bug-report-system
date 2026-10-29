package com.ramy.bugreport.security;

import java.util.Map;

import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.ramy.bugreport.domain.UserAccount;

/** OIDC identity plus the local account expected by controllers and ownership checks. */
public class GoogleUserAccountDetails extends UserAccountDetails implements OidcUser {
    private static final long serialVersionUID = 1L;
    private final OidcUser googleUser;

    public GoogleUserAccountDetails(UserAccount account, OidcUser googleUser) {
        super(account);
        this.googleUser = googleUser;
    }

    public GoogleUserAccountDetails withAccount(UserAccount account) {
        return new GoogleUserAccountDetails(account, googleUser);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return googleUser.getAttributes();
    }

    @Override
    public Map<String, Object> getClaims() {
        return googleUser.getClaims();
    }

    @Override
    public OidcIdToken getIdToken() {
        return googleUser.getIdToken();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return googleUser.getUserInfo();
    }
}
