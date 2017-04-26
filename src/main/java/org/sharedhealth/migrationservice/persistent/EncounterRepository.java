package org.sharedhealth.migrationservice.persistent;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.stereotype.Component;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;


@Component
public class EncounterRepository {
    private final String ENCOUNTER_ID_COLUMN_NAME = "encounter_id";

    private CqlOperations cqlOperations;
    private SHRMigrationProperties shrProperties;
    private String ENCOUNTER_TABLE_NAME = "encounter";

    @Autowired
    public EncounterRepository(@Qualifier("SHRCassandraTemplate") CqlOperations cassandraTemplate, SHRMigrationProperties shrProperties) {
        this.cqlOperations = cassandraTemplate;
        this.shrProperties = shrProperties;
    }

    public void save(String stu3BundleContent, String encounterId) {
        Update.Where update = QueryBuilder.update(ENCOUNTER_TABLE_NAME)
                .with(set(getContentColumnName(), stu3BundleContent))
                .where(eq(ENCOUNTER_ID_COLUMN_NAME, encounterId));

        cqlOperations.execute(update);
    }

    private String getContentColumnName() {
        return "content_" + shrProperties.getFhirDocumentSchemaVersion();
    }
}
