package org.sharedhealth.migrationservice.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {"org.sharedhealth.migrationservice"})
@Import(SHRCassandraConfig.class)
public class SHRMigrationConfig {
}
