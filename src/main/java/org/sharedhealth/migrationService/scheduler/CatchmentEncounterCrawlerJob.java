package org.sharedhealth.migrationService.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ict4h.atomfeed.client.repository.jdbc.AllFailedEventsJdbcImpl;
import org.ict4h.atomfeed.client.repository.jdbc.AllMarkersJdbcImpl;
import org.sharedhealth.migrationService.client.ShrClient;
import org.sharedhealth.migrationService.config.SHRMigrationProperties;
import org.sharedhealth.migrationService.feed.encounter.EncounterEventWorker;
import org.sharedhealth.migrationService.feed.encounter.ShrCatchmentEncounterFeedProcessor;
import org.sharedhealth.migrationService.feed.transaction.AtomFeedSpringTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CatchmentEncounterCrawlerJob {
    @Autowired
    private DataSourceTransactionManager txMgr;
    @Autowired
    private SHRMigrationProperties properties;
    @Autowired
    private ShrClient shrWebClient;
    @Autowired
    private EncounterEventWorker encounterEventWorker;

    private Logger logger = LogManager.getLogger(CatchmentEncounterCrawlerJob.class);

    @Scheduled(fixedDelayString = "${ENCOUNTER_SYNC_JOB_INTERVAL}", initialDelay = 10000)
    public void start() {
        for (String catchment : properties.getCatchmentList()) {
            String feedUrl = properties.getShrServerBaseUrl() + "catchments/" + catchment + "/encounters";
            AtomFeedSpringTransactionManager transactionManager = new AtomFeedSpringTransactionManager(txMgr);
            ShrCatchmentEncounterFeedProcessor feedCrawler =
                    new ShrCatchmentEncounterFeedProcessor(
                            encounterEventWorker, feedUrl,
                            new AllMarkersJdbcImpl(transactionManager),
                            new AllFailedEventsJdbcImpl(transactionManager),
                            transactionManager, shrWebClient, properties);
            try {
                logger.info("Crawling feed:" + feedUrl);
                feedCrawler.process();
            } catch (Exception e) {
                String errorMessage = String.format("Unable to process encounter catchment feed [%s]", feedUrl);
                logger.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            }
        }
    }
}
