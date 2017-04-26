package org.sharedhealth.migrationservice.identity;

import org.springframework.stereotype.Component;

@Component("identityStore")
public class IdentityStore {
    IdentityToken token;

    public IdentityToken getToken() {
        return token;
    }

    public void setToken(IdentityToken token) {
        this.token = token;
    }

    public void clearToken() {
        this.token = null;
    }
}
