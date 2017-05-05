package org.sharedhealth.migrationservice.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ict4h.atomfeed.client.repository.jdbc.AllMarkersJdbcImpl;
import org.sharedhealth.migrationservice.client.ShrClient;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;
import org.sharedhealth.migrationservice.feed.encounter.EncounterEventWorker;
import org.sharedhealth.migrationservice.feed.encounter.ShrCatchmentEncounterFeedProcessor;
import org.sharedhealth.migrationservice.feed.transaction.AtomFeedSpringTransactionManager;
import org.sharedhealth.migrationservice.feed.transaction.SHRFailedEventsJdbcImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CatchmentEncounterCrawlerJob {
    private final DataSourceTransactionManager txMgr;
    private final SHRMigrationProperties properties;
    private final ShrClient shrWebClient;
    private final EncounterEventWorker encounterEventWorker;
    private static final Logger logger = LogManager.getLogger(CatchmentEncounterCrawlerJob.class);

    @Autowired
    public CatchmentEncounterCrawlerJob(DataSourceTransactionManager txMgr, SHRMigrationProperties properties, ShrClient shrWebClient, EncounterEventWorker encounterEventWorker) {
        this.txMgr = txMgr;
        this.properties = properties;
        this.shrWebClient = shrWebClient;
        this.encounterEventWorker = encounterEventWorker;
    }

    @Scheduled(fixedDelayString = "${ENCOUNTER_SYNC_JOB_INTERVAL}", initialDelay = 10000)
    public void start() {
        for (String catchment : properties.getCatchmentList()) {
            String feedUrl = properties.getShrServerBaseUrl() + "catchments/" + catchment + "/encounters";
            AtomFeedSpringTransactionManager transactionManager = new AtomFeedSpringTransactionManager(txMgr);
            ShrCatchmentEncounterFeedProcessor feedCrawler =
                    new ShrCatchmentEncounterFeedProcessor(
                            encounterEventWorker, feedUrl,
                            new AllMarkersJdbcImpl(transactionManager),
                            new SHRFailedEventsJdbcImpl(transactionManager, properties),
                            transactionManager, shrWebClient, properties);
            try {
                logger.info("Crawling feed:" + feedUrl);
                feedCrawler.process();
                feedCrawler.processFailedEvents();
            } catch (Exception e) {
                String errorMessage = String.format("Unable to process encounter catchment feed [%s]", feedUrl);
                logger.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            }
        }
    }
}
