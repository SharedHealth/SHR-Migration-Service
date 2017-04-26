package org.sharedhealth.migrationservice.config;

import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@EnableTransactionManagement
@Configuration
public class AtomFeedClientDatabaseConfig {

    @Autowired
    private SHRMigrationProperties properties;

    @Bean
    public DataSource dataSource() {

        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(properties.getShrMigrationDatabaseUrl());
        basicDataSource.setUsername(properties.getShrMigrationDbUser());
        basicDataSource.setPassword(properties.getShrMigrationDbPassword());
        basicDataSource.setDriverClassName(properties.getShrMigrationDbDriver());
        basicDataSource.setInitialSize(Integer.parseInt(properties.getDbPoolSize()));
        return basicDataSource;
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public SpringLiquibase liquibase() {
        String changelogFile = properties.getShrMigrationDbChangeLogFile();
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog(changelogFile);
        liquibase.setIgnoreClasspathPrefix(false);
        liquibase.setDataSource(dataSource());
        liquibase.setDropFirst(false);
        liquibase.setShouldRun(true);
        return liquibase;
    }
}
