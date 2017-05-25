package org.sharedhealth.migrationservice.persistent;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;
import org.sharedhealth.migrationservice.model.EncounterDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.stereotype.Component;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;


@Component
public class EncounterRepository {
    private final String ENCOUNTER_ID_COLUMN_NAME = "encounter_id";
    private final String RECEIVED_AT_COLUMN_NAME = "received_at";

    private CqlOperations cqlOperations;
    private SHRMigrationProperties shrProperties;
    private String ENCOUNTER_TABLE_NAME = "encounter";

    @Autowired
    public EncounterRepository(@Qualifier("SHRCassandraTemplate") CqlOperations cassandraTemplate, SHRMigrationProperties shrProperties) {
        this.cqlOperations = cassandraTemplate;
        this.shrProperties = shrProperties;
    }

    public void save(String stu3BundleContent, String dstu2BundleContent, EncounterDetails encounterDetails) {
        Update.Where update = QueryBuilder.update(ENCOUNTER_TABLE_NAME)
                .with(set(getNewContentColumnName(), stu3BundleContent))
                .and(set(getOldContentColumnName(), dstu2BundleContent))
                .and(set(getNewContentVersionColumnName(), encounterDetails.getOldContentVersion()))
                .where(eq(ENCOUNTER_ID_COLUMN_NAME, encounterDetails.getEncounterId()))
                .and(eq(RECEIVED_AT_COLUMN_NAME, encounterDetails.getReceivedAt()));

        cqlOperations.execute(update);
    }

    private String getNewContentColumnName() {
        return "content_" + shrProperties.getFhirDocumentNewSchemaVersion();
    }

    private String getOldContentColumnName() {return "content_" + shrProperties.getFhirDocumentOldSchemaVersion();}

    private String getNewContentVersionColumnName() {
        return "content_version_" + shrProperties.getFhirDocumentNewSchemaVersion();
    }

    private String getOldContentVersionColumnName() {
        return "content_version_" + shrProperties.getFhirDocumentOldSchemaVersion();
    }

    public EncounterDetails getByEncounterId(String encounterId) {
        String oldVersionColumnName = getOldContentVersionColumnName();
        Select select = QueryBuilder.select(ENCOUNTER_ID_COLUMN_NAME, RECEIVED_AT_COLUMN_NAME, oldVersionColumnName)
                .from(ENCOUNTER_TABLE_NAME)
                .where(eq(ENCOUNTER_ID_COLUMN_NAME, encounterId))
                .limit(1);

        return cqlOperations.query(select, (row, rowNum) -> new EncounterDetails(row.getString(ENCOUNTER_ID_COLUMN_NAME),
                row.getUUID(RECEIVED_AT_COLUMN_NAME), row.getInt(oldVersionColumnName))
        ).get(0);
    }
}
