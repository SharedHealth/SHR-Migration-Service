package org.sharedhealth.migrationService.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.sharedhealth.migrationService.utils.StringUtil.ensureSuffix;


@Configuration
public class SHRMigrationProperties {

    @Value("${SHR_SERVER_BASE_URL}")
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

    @Value("${DATABASE_URL}")
    private String dbUrl;

    @Value("${DATABASE_USER}")
    private String dbUser;

    @Value("${DATABASE_PASSWORD}")
    private String dbPassword;

    @Value("${DATABASE_CHANGELOG_FILE}")
    private String dbChangeLogFile;

    @Value("${DATABASE_DRIVER}")
    private String dbDriver;

    @Value("${DATABASE_CON_POOL_SIZE}")
    private String dbPoolSize;

    @Value("${ENCOUNTER_SYNC_JOB_INTERVAL}")
    private String encSyncJobInterval;

    @Value("${CATCHMENT_LIST}")
    private String catchmentList;

    @Value("${TR_VALUESET_URI}")
    private String trValuesetUri;

    public int getEncSyncJobInterval() {
        return Integer.parseInt(encSyncJobInterval);
    }

    public String[] getCatchmentList() {
        return StringUtils.split(catchmentList, ",");
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public String getDbPoolSize() {
        return dbPoolSize;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getDbChangeLogFile() {
        return dbChangeLogFile;
    }

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

    public String getTrValuesetUri() {
        return ensureSuffix(trValuesetUri, "/");
    }

    public String getShrServerBaseUrl() {
        return ensureSuffix(shrUrl, "/");
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
