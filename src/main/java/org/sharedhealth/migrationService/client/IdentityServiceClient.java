package org.sharedhealth.migrationService.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sharedhealth.migrationService.config.SHRMigrationProperties;
import org.sharedhealth.migrationService.identity.IdentityStore;
import org.sharedhealth.migrationService.identity.IdentityToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.sharedhealth.migrationService.utils.Headers.*;


@Component
public class IdentityServiceClient {
    private SHRMigrationProperties properties;
    private IdentityStore identityStore;

    @Autowired
    public IdentityServiceClient(SHRMigrationProperties properties, IdentityStore identityStore) {
        this.properties = properties;
        this.identityStore = identityStore;
    }

    public IdentityToken getOrCreateToken() throws IOException {
        IdentityToken token = identityStore.getToken();
        if (token == null) {
            Map<String, String> headers = getIdpServerHeaders(properties);
            headers.put("accept", "application/json");
            Map<String, String> clientCredentials = new HashMap<>();
            clientCredentials.put(EMAIL_KEY, properties.getIdpClientEmail());
            clientCredentials.put(PASSWORD_KEY, properties.getIdpClientPassword());
                String response = new WebClient().post(properties.getIdpServerLoginUrl(), clientCredentials, headers);
                token = readFrom(response, IdentityToken.class);
            identityStore.setToken(token);
        }
        return token;
    }


    private static <T> T readFrom(String content, Class<T> returnType) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(content, returnType);

    }

    public void clearToken() {
        identityStore.clearToken();
    }

}
