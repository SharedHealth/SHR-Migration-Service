package org.sharedhealth.migrationService.feed.encounter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ict4h.atomfeed.client.AtomFeedProperties;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.repository.AllFailedEvents;
import org.ict4h.atomfeed.client.repository.AllMarkers;
import org.ict4h.atomfeed.client.service.AtomFeedClient;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.sharedhealth.migrationService.client.ShrClient;
import org.sharedhealth.migrationService.config.ShrProperties;
import org.sharedhealth.migrationService.feed.transaction.AtomFeedSpringTransactionManager;

import java.net.URI;
import java.net.URISyntaxException;

public class ShrCatchmentEncounterFeedProcessor {

    private EncounterEventWorker encounterEventWorker;
    private String feedUrl;
    private AllMarkers markers;
    private AllFailedEvents failedEvents;
    private AtomFeedSpringTransactionManager transactionManager;
    private ShrClient shrWebClient;
    private ShrProperties properties;

    private Logger logger = LogManager.getLogger(ShrCatchmentEncounterFeedProcessor.class);


    public ShrCatchmentEncounterFeedProcessor(EncounterEventWorker encounterEventWorker,
                                              String feedUrl,
                                              AllMarkers markers,
                                              AllFailedEvents failedEvents,
                                              AtomFeedSpringTransactionManager transactionManager,
                                              ShrClient shrWebClient,
                                              ShrProperties properties) {
        this.encounterEventWorker = encounterEventWorker;
        this.feedUrl = feedUrl;
        this.markers = markers;
        this.failedEvents = failedEvents;
        this.transactionManager = transactionManager;
        this.shrWebClient = shrWebClient;
        this.properties = properties;
    }

    public void process() throws URISyntaxException {
        AtomFeedProperties atomProperties = new AtomFeedProperties();
        AtomFeedClient atomFeedClient = atomFeedClient(new URI(this.feedUrl),
                new FeedEventWorker(encounterEventWorker),
                atomProperties);
        logger.debug("Crawling feed:" + this.feedUrl);
        atomFeedClient.processEvents();
    }

    private AtomFeedClient atomFeedClient(URI feedUri, EventWorker worker, AtomFeedProperties atomProperties) {
        return new AtomFeedClient(
                new AllEncounterFeeds(shrWebClient),
                markers,
                failedEvents,
                atomProperties,
                transactionManager,
                feedUri,
                worker);
    }

    private class FeedEventWorker implements EventWorker {
        private EncounterEventWorker encounterEventWorker;

        FeedEventWorker(EncounterEventWorker encounterEventWorker) {
            this.encounterEventWorker = encounterEventWorker;
        }

        @Override
        public void process(Event event) {
            logger.info(event.getFeedUri());
            logger.info(event.getId());
            String content = event.getContent();
            encounterEventWorker.process(content);
        }


        @Override
        public void cleanUp(Event event) {
        }
    }

    //Title -> Encounter:shrEncounterId
    private String getSHREncounterId(String eventTitle) {
        return StringUtils.substringAfter(eventTitle, "Encounter:");
    }
}
