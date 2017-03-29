package org.sharedhealth.migrationService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;


@Configuration
public class ShrProperties {

    @Value("$SHR_SERVER_BASE_URL")
    private String shrUrl;

    @Value("${IDP_SERVER_LOGIN_URL}")
    private String idpServerLoginUrl;

    @Value("${IDP_CLIENT_ID}")
    private String idpClientId;

    @Value("${IDP_AUTH_TOKEN}")
    private String idpClientAuthToken;

    @Value("${IDP_CLIENT_EMAIL}")
    private String idpClientEmail;

    @Value("${IDP_CLIENT_PASSWORD}")
    private String idpClientPassword;

    public String getIdpServerLoginUrl() {
        return idpServerLoginUrl;
    }

    public String getIdpClientId() {
        return idpClientId;
    }

    public String getIdpClientAuthToken() {
        return idpClientAuthToken;
    }

    public String getIdpClientEmail() {
        return idpClientEmail;
    }

    public String getIdpClientPassword() {
        return idpClientPassword;
    }


    public String getShrServerBaseUrl() {
        return shrUrl;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
