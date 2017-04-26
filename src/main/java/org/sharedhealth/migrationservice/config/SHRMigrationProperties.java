package org.sharedhealth.migrationservice.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.sharedhealth.migrationservice.utils.StringUtil.ensureSuffix;


@Configuration
public class SHRMigrationProperties {

    @Value("${SHR_SERVER_BASE_URL}")
    private String shrUrl;
    @Value("${FHIR_DOCUMENT_SCHEMA_VERSION}")
    private String fhirDocumentSchemaVersion;

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

    @Value("${SHR_MIGRATION_DATABASE_URL}")
    private String shrMigrationDatabaseUrl;
    @Value("${SHR_MIGRATION_DATABASE_USER}")
    private String shrMigrationDbUser;
    @Value("${SHR_MIGRATION_DATABASE_PASSWORD}")
    private String shrMigrationDbPassword;
    @Value("${SHR_MIGRATION_DATABASE_CHANGELOG_FILE}")
    private String shrMigrationDbChangeLogFile;
    @Value("${SHR_MIGRATION_DATABASE_DRIVER}")
    private String shrMigrationDbDriver;
    @Value("${SHR_MIGRATION_DATABASE_CON_POOL_SIZE}")
    private String dbPoolSize;

    @Value("${CASSANDRA_USER}")
    private String cassandraUser;
    @Value("${CASSANDRA_PASSWORD}")
    private String cassandraPassword;
    @Value("${CASSANDRA_KEYSPACE}")
    private String cassandraKeySpace;
    @Value("${CASSANDRA_HOST}")
    private String cassandraHost;
    @Value("${CASSANDRA_PORT}")
    private int cassandraPort;
    @Value("${CASSANDRA_TIMEOUT}")
    private int cassandraTimeout;

    @Value("${ENCOUNTER_SYNC_JOB_INTERVAL}")
    private String encSyncJobInterval;
    @Value("${CATCHMENT_LIST}")
    private String catchmentList;
    @Value("${TR_VALUESET_URI}")
    private String trValuesetUri;

    public String getCassandraUser() {
        return cassandraUser;
    }

    public String getCassandraPassword() {
        return cassandraPassword;
    }

    public String getCassandraKeySpace() {
        return cassandraKeySpace;
    }

    public String getCassandraHost() {
        return cassandraHost;
    }

    public int getCassandraPort() {
        return cassandraPort;
    }

    public int getCassandraTimeout() {
        return cassandraTimeout;
    }

    public int getEncSyncJobInterval() {
        return Integer.parseInt(encSyncJobInterval);
    }

    public String[] getCatchmentList() {
        return StringUtils.split(catchmentList, ",");
    }

    public String getShrMigrationDbDriver() {
        return shrMigrationDbDriver;
    }

    public String getDbPoolSize() {
        return dbPoolSize;
    }

    public String getShrMigrationDatabaseUrl() {
        return shrMigrationDatabaseUrl;
    }

    public String getShrMigrationDbUser() {
        return shrMigrationDbUser;
    }

    public String getShrMigrationDbPassword() {
        return shrMigrationDbPassword;
    }

    public String getShrMigrationDbChangeLogFile() {
        return shrMigrationDbChangeLogFile;
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

    public String getFhirDocumentSchemaVersion() {
        return fhirDocumentSchemaVersion;
    }
}
