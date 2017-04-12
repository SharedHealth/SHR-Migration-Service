package org.sharedhealth.migrationService.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.migrationService.config.SHRMigrationProperties;
import org.sharedhealth.migrationService.exception.ConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.sharedhealth.migrationService.utils.Headers.getShrIdentityHeaders;

@Component
public class ShrClient {
    private IdentityServiceClient identityServiceClient;

    private SHRMigrationProperties properties;
    private Logger logger = LogManager.getLogger(ShrClient.class);


    @Autowired
    public ShrClient(IdentityServiceClient identityServiceClient, SHRMigrationProperties properties) {
        this.identityServiceClient = identityServiceClient;
        this.properties = properties;
    }

    public String getFeed(URI url) throws IOException {
        logger.debug("Reading from " + url);
        Map<String, String> headers = getShrIdentityHeaders(identityServiceClient.getOrCreateToken(), properties);
        String response = null;
        try {
            response = new WebClient().get(url, headers);
        } catch (ConnectionException e) {
            logger.error(String.format("Could not fetch. Exception: %s", e));
            if (e.getErrorCode() == 401) {
                logger.error("Unauthorized, clearing token.");
                identityServiceClient.clearToken();
            }else{
                throw new RuntimeException(e);
            }
        }
        return response;
    }

}
