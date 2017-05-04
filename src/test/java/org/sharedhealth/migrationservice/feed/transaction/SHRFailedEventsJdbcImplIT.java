package org.sharedhealth.migrationservice.feed.transaction;

import org.apache.commons.io.FileUtils;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.domain.FailedEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.migrationservice.config.SHREnvironmentMock;
import org.sharedhealth.migrationservice.config.SHRMigrationConfig;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRMigrationConfig.class)
public class SHRFailedEventsJdbcImplIT {
    @Autowired
    private SHRMigrationProperties migrationProperties;
    @Autowired
    private DataSourceTransactionManager txMgr;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SHRFailedEventsJdbcImpl shrFailedEventsJdbc;

    @Before
    public void setUp() throws Exception {
        AtomFeedSpringTransactionManager transactionManager = new AtomFeedSpringTransactionManager(txMgr);
        shrFailedEventsJdbc = new SHRFailedEventsJdbcImpl(transactionManager, migrationProperties);

        String bundleStorageDirPath = migrationProperties.getFailedBundleStorageDirPath();
        new File(bundleStorageDirPath).mkdir();
    }

    @Test
    public void shouldWriteEventContentToFileAndStoreFileLocation() throws Exception {
        String content = "A Bundle";
        String feedUri = "http:shr.com";
        Event event = new Event("1", content, "enc-123", feedUri, new Date());
        shrFailedEventsJdbc.addOrUpdate(new FailedEvent(feedUri, event, "error", 0));

        List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from failed_events");
        assertEquals(1, list.size());
        Map<String, Object> failedEvent = list.get(0);
        assertEquals(feedUri, failedEvent.get("feed_uri"));
        String eventContent = (String) failedEvent.get("event_content");
        assertNotEquals(content, eventContent);

        assertTrue(new File(eventContent).exists());
        assertEquals(content, FileUtils.readFileToString(new File(eventContent), "UTF-8"));
    }

    @Test
    public void shouldPopulateEventContentFromSavedFile() throws Exception {
        String content = "A Bundle";
        String feedUri = "http:shr.com";
        Event event = new Event("1", content, "enc-123", feedUri, new Date());
        shrFailedEventsJdbc.addOrUpdate(new FailedEvent(feedUri, event, "error", 0));

        FailedEvent failedEvent = shrFailedEventsJdbc.getOldestNFailedEvents(feedUri, 1, 1).get(0);
        assertEquals(content, failedEvent.getEvent().getContent());
    }

    @After
    public void tearDown() throws Exception {
        jdbcTemplate.execute("delete from markers");
        jdbcTemplate.execute("delete from failed_events");
        jdbcTemplate.execute("delete from failed_event_retry_log");

        String bundleStorageDirPath = migrationProperties.getFailedBundleStorageDirPath();
        File file = new File(bundleStorageDirPath);
        FileUtils.cleanDirectory(file);
        file.delete();
    }

}
