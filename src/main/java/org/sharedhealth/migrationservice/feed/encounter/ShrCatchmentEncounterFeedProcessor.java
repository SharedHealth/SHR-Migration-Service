package org.sharedhealth.migrationservice.feed.encounter;

import com.sun.syndication.feed.atom.Category;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ict4h.atomfeed.client.AtomFeedProperties;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.repository.AllFailedEvents;
import org.ict4h.atomfeed.client.repository.AllMarkers;
import org.ict4h.atomfeed.client.service.AtomFeedClient;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.sharedhealth.migrationservice.client.ShrClient;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;
import org.sharedhealth.migrationservice.feed.transaction.AtomFeedSpringTransactionManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ShrCatchmentEncounterFeedProcessor {

    private EncounterEventWorker encounterEventWorker;
    private String feedUrl;
    private AllMarkers markers;
    private AllFailedEvents failedEvents;
    private AtomFeedSpringTransactionManager transactionManager;
    private ShrClient shrWebClient;

    private Logger logger = LogManager.getLogger(ShrCatchmentEncounterFeedProcessor.class);


    public ShrCatchmentEncounterFeedProcessor(EncounterEventWorker encounterEventWorker,
                                              String feedUrl,
                                              AllMarkers markers,
                                              AllFailedEvents failedEvents,
                                              AtomFeedSpringTransactionManager transactionManager,
                                              ShrClient shrWebClient,
                                              SHRMigrationProperties properties) {
        this.encounterEventWorker = encounterEventWorker;
        this.feedUrl = feedUrl;
        this.markers = markers;
        this.failedEvents = failedEvents;
        this.transactionManager = transactionManager;
        this.shrWebClient = shrWebClient;
    }

    public void process() throws URISyntaxException {
        AtomFeedProperties atomProperties = new AtomFeedProperties();
        AtomFeedClient atomFeedClient = atomFeedClient(new URI(this.feedUrl),
                new FeedEventWorker(encounterEventWorker),
                atomProperties);
        atomFeedClient.processEvents();
    }

    public void processFailedEvents() throws URISyntaxException {
        AtomFeedProperties atomProperties = new AtomFeedProperties();
        atomProperties.setFailedEventMaxRetry(1);
        AtomFeedClient atomFeedClient = atomFeedClient(new URI(this.feedUrl),
                new FeedEventWorker(encounterEventWorker),
                atomProperties);
        atomFeedClient.processFailedEvents();
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
            logger.debug(String.format("Processing event with id %s", event.getId()));
            if (hasUpdatedEventInFeed(event)) {
                logger.info(String.format("The event with id %s has been updated later, skipping", event.getId()));
                return;
            }
            String content = extractContent(event.getContent());
            if (StringUtils.isBlank(content)) {
                logger.info(String.format("The event with title %s doesn't have any content, skipping", event.getTitle()));
                return;
            }
            encounterEventWorker.process(content, getSHREncounterId(event.getTitle()));
        }


        @Override
        public void cleanUp(Event event) {
        }
    }

    //Title -> Encounter:shrEncounterId
    private String getSHREncounterId(String eventTitle) {
        return StringUtils.substringAfter(eventTitle, "Encounter:");
    }

    private static String extractContent(String content) {
        return content.trim().replaceFirst(
                "^<!\\[CDATA\\[", "").replaceFirst("\\]\\]>$", "");
    }

    private boolean hasUpdatedEventInFeed(Event event) {
        String latestUpdateEventId = "latest_update_event_id";
        List allCategories = event.getCategories();
        if (allCategories != null) {
            for (Object category : allCategories) {
                if (((Category) category).getTerm().startsWith(latestUpdateEventId))
                    return true;
            }
        }
        return false;
    }


}
