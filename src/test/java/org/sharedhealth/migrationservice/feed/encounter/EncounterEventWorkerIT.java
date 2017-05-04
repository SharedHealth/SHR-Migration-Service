package org.sharedhealth.migrationservice.feed.encounter;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.migrationservice.config.SHREnvironmentMock;
import org.sharedhealth.migrationservice.config.SHRMigrationConfig;
import org.sharedhealth.migrationservice.utils.TimeUuidUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRMigrationConfig.class)
public class EncounterEventWorkerIT {
    @Autowired
    private EncounterEventWorker encounterEventWorker;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("SHRCassandraTemplate")
    private CqlOperations cqlOperations;

    @Test
    public void shouldConvertAnEncounterBundleAndSaveToDatabase() throws Exception {
        String encounterId = "shr-enc-1";
        Insert insert = QueryBuilder.insertInto("encounter")
                .value("encounter_id", encounterId)
                .value("received_at", TimeUuidUtil.uuidForDate(new Date()))
                .value("content_version_v2", 1)
                .value("content_v2", "ABCD");

        cqlOperations.execute(insert);

        Select select = QueryBuilder.select().all().from("encounter");
        List<Row> all = cqlOperations.query(select).all();
        assertEquals(1, all.size());
        Row firstRow = all.get(0);
        assertEquals(encounterId, firstRow.getString("encounter_id"));
        assertNull(firstRow.getString("content_v3"));

        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_encounter.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        encounterEventWorker.process(content, encounterId);

        List<Row> afterUpdateAll = cqlOperations.query(QueryBuilder.select().all().from("encounter")).all();
        assertEquals(1, afterUpdateAll.size());
        Row afterUpdateFirstRow = afterUpdateAll.get(0);
        assertEquals(encounterId, afterUpdateFirstRow.getString("encounter_id"));
        assertNotNull(afterUpdateFirstRow.getString("content_v3"));
        assertEquals(afterUpdateFirstRow.getInt("content_version_v2"), afterUpdateFirstRow.getInt("content_version_v3"));
    }


    @After
    public void tearDown() throws Exception {
        jdbcTemplate.execute("delete from markers");
        jdbcTemplate.execute("delete from failed_events");
        jdbcTemplate.execute("delete from failed_event_retry_log");

        cqlOperations.execute("truncate encounter");
        cqlOperations.execute("truncate enc_by_catchment");
        cqlOperations.execute("truncate enc_by_patient");
        cqlOperations.execute("truncate enc_history");
        cqlOperations.execute("truncate patient");
    }

}
