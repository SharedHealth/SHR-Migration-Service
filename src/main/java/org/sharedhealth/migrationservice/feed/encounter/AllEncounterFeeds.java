package org.sharedhealth.migrationservice.feed.encounter;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.WireFeedInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ict4h.atomfeed.client.repository.AllFeeds;
import org.sharedhealth.migrationservice.client.ShrClient;

import java.io.StringReader;
import java.net.URI;

public class AllEncounterFeeds extends AllFeeds {

    private ShrClient shrClient;
    private Logger logger = LogManager.getLogger(ShrClient.class);



    public AllEncounterFeeds(ShrClient shrWebClient) {
        this.shrClient = shrWebClient;
    }

    @Override
    public Feed getFor(URI uri) {
        try {
            String response = shrClient.getFeed(uri);
            WireFeedInput input = new WireFeedInput();
            return (Feed) input.build(new StringReader(response));
        } catch (Exception e) {
            logger.error(String.format("Error occurred while processing feed for uri %s", uri.toString()), e);
            throw new RuntimeException(e);
        }
    }

}
