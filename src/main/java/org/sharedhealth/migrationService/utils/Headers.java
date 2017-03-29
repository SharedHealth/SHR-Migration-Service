package org.sharedhealth.migrationService.utils;

import org.sharedhealth.migrationService.config.ShrProperties;
import org.sharedhealth.migrationService.identity.IdentityToken;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Headers {
    public final static String FROM_KEY = "From";
    public final static String EMAIL_KEY = "email";
    public final static String PASSWORD_KEY = "password";
    public final static String CLIENT_ID_KEY = "client_id";
    public final static String X_AUTH_TOKEN_KEY = "X-Auth-Token";

    public static Map<String, String> getIdpServerHeaders(ShrProperties properties) {
        Map<String, String> headers = new HashMap<>();
        headers.put(X_AUTH_TOKEN_KEY, properties.getIdpClientAuthToken());
        headers.put(CLIENT_ID_KEY, properties.getIdpClientId());
        return headers;
    }

    public static Map<String, String> getShrIdentityHeaders(IdentityToken identityToken, ShrProperties properties) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(X_AUTH_TOKEN_KEY, identityToken.toString());
        headers.put(CLIENT_ID_KEY, properties.getIdpClientId());
        headers.put(FROM_KEY, properties.getIdpClientEmail());
        headers.put("Accept", "application/atom+xml");
        return headers;
    }

}
