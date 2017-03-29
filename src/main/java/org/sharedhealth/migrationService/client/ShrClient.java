package org.sharedhealth.migrationService.client;

import org.sharedhealth.migrationService.config.ShrProperties;
import org.sharedhealth.migrationService.exception.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.sharedhealth.migrationService.utils.Headers.getShrIdentityHeaders;

@Component
public class ShrClient {
    private IdentityServiceClient identityServiceClient;
    private ShrProperties properties;
    private Logger log = LoggerFactory.getLogger(ShrClient.class);

    @Autowired
    public ShrClient(IdentityServiceClient identityServiceClient, ShrProperties properties) {
        this.identityServiceClient = identityServiceClient;
        this.properties = properties;
    }

    public String getFeed(URI url) throws IOException {
        log.debug("Reading from " + url);
        Map<String, String> headers = getShrIdentityHeaders(identityServiceClient.getOrCreateToken(), properties);
        String response = null;
        try {
            response = new WebClient().get(url, headers);
        } catch (ConnectionException e) {
            log.error(String.format("Could not fetch. Exception: %s", e));
            if (e.getErrorCode() == 401) {
                log.error("Unauthorized, clearing token.");
                identityServiceClient.clearToken();
            }else{
                throw new RuntimeException(e);
            }
        }
        return response;
    }

}
