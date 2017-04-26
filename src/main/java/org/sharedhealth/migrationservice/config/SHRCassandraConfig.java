package org.sharedhealth.migrationservice.config;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.SocketOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.cassandra.core.CqlTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@Configuration
@EnableCassandraRepositories(basePackages = "org.sharedhealth.migrationservice.persistent")
public class SHRCassandraConfig extends AbstractCassandraConfiguration {

    @Autowired
    private SHRMigrationProperties migrationProperties;

    @Override
    protected String getKeyspaceName() {
        return migrationProperties.getCassandraKeySpace();
    }

    @Override
    protected String getContactPoints() {
        return migrationProperties.getCassandraHost();
    }

    @Override
    protected int getPort() {
        return migrationProperties.getCassandraPort();
    }

    @Override
    protected AuthProvider getAuthProvider() {
        return new PlainTextAuthProvider(migrationProperties.getCassandraUser(), migrationProperties.getCassandraPassword());
    }

    @Override
    protected SocketOptions getSocketOptions() {
        SocketOptions socketOptions = new SocketOptions();
        socketOptions.setConnectTimeoutMillis(migrationProperties.getCassandraTimeout());
        socketOptions.setReadTimeoutMillis(migrationProperties.getCassandraTimeout());
        return socketOptions;
    }

    @Bean(name = "SHRCassandraTemplate")
    public CqlOperations CassandraTemplate() throws Exception {
        return new CqlTemplate(session().getObject());
    }

}
