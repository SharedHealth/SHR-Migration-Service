package org.sharedhealth.migrationservice.feed.encounter;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpStatus;
import org.ict4h.atomfeed.client.repository.jdbc.AllFailedEventsJdbcImpl;
import org.ict4h.atomfeed.client.repository.jdbc.AllMarkersJdbcImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.migrationservice.client.ShrClient;
import org.sharedhealth.migrationservice.config.SHREnvironmentMock;
import org.sharedhealth.migrationservice.config.SHRMigrationConfig;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;
import org.sharedhealth.migrationservice.feed.transaction.AtomFeedSpringTransactionManager;
import org.sharedhealth.migrationservice.utils.TimeUuidUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.sharedhealth.migrationservice.helpers.ResourceHelper.asString;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRMigrationConfig.class)
public class ShrCatchmentEncounterFeedProcessorIT {
    private ShrCatchmentEncounterFeedProcessor feedProcessor;
    private String access_token = "access_token";

    @Autowired
    private DataSourceTransactionManager txMgr;
    @Autowired
    private SHRMigrationProperties properties;
    @Autowired
    private ShrClient shrWebClient;
    @Autowired
    private EncounterEventWorker encounterEventWorker;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("SHRCassandraTemplate")
    private CqlOperations cqlOperations;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);

    @Before
    public void setUp() throws Exception {
        stubFor(post(urlMatching("/signin"))
                .willReturn(
                        aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody("{\"access_token\": \"" + access_token + "\"}")
                )
        );

        stubFor(get(urlMatching("/encounters"))
                .willReturn(
                        aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody(asString("feeds/encounterByCatchmentFeed.xml"))
                )
        );

        AtomFeedSpringTransactionManager transactionManager = new AtomFeedSpringTransactionManager(txMgr);
        feedProcessor = new ShrCatchmentEncounterFeedProcessor(encounterEventWorker, "http://localhost:9997/encounters",
                new AllMarkersJdbcImpl(transactionManager), new AllFailedEventsJdbcImpl(transactionManager),
                transactionManager, shrWebClient, properties);
    }


    @Test
    public void shouldProcessEncounterEventAndSaveTheMarker() throws Exception {
        String encounterId = "bc55a875-f163-4e79-aa53-9863f75b6ce3";
        Insert insert = QueryBuilder.insertInto("encounter")
                .value("encounter_id", encounterId)
                .value("received_at", TimeUuidUtil.uuidForDate(new Date()))
                .value("content_v2", "ABCD");

        cqlOperations.execute(insert);

        Select select = QueryBuilder.select().all().from("encounter");
        List<Row> all = cqlOperations.query(select).all();
        assertEquals(1, all.size());
        Row firstRow = all.get(0);
        assertEquals(encounterId, firstRow.getString("encounter_id"));
        assertNull(firstRow.getString("content_v3"));

        feedProcessor.process();

        List<Row> afterUpdateAll = cqlOperations.query(QueryBuilder.select().all().from("encounter")).all();
        assertEquals(1, afterUpdateAll.size());
        Row afterUpdateFirstRow = afterUpdateAll.get(0);
        assertEquals(encounterId, afterUpdateFirstRow.getString("encounter_id"));
        assertNotNull(afterUpdateFirstRow.getString("content_v3"));

        assertMarkers();
    }

    @Test
    public void shouldPutInFailedEventsWhenProcessFails() throws Exception {
        feedProcessor.process();
        assertMarkers();

        List<Map<String, String>> failedEvents = jdbcTemplate.query("select * from failed_events", (rs, rowNum) -> {
            Map<String, String> map = new HashMap<>();
            map.put("title", String.valueOf(rs.getString("title")));
            return map;
        });

        assertEquals(1, failedEvents.size());
        Map<String, String> failedEvent = failedEvents.get(0);
        assertEquals("Encounter:bc55a875-f163-4e79-aa53-9863f75b6ce3", failedEvent.get("title"));
    }

    private void assertMarkers() {
        List<Map<String, String>> markers = jdbcTemplate.query("select * from markers", (rs, rowNum) -> {
            Map<String, String> map = new HashMap<>();
            map.put("start_uri", rs.getString("feed_uri"));
            map.put("last_marker", rs.getString("last_read_entry_id"));
            map.put("last_uri", rs.getString("feed_uri_for_last_read_entry"));
            return map;
        });

        assertEquals(1, markers.size());
        Map<String, String> marker = markers.get(0);
        assertEquals("http://localhost:9997/encounters", marker.get("start_uri"));
        assertEquals("http://localhost:9997/encounters?updatedSince=2017-03-01T00%3A00%3A00.000%2B0530", marker.get("last_uri"));
        assertEquals("df41ae40-087c-11e7-9fea-667d3bb0eee2", marker.get("last_marker"));
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
