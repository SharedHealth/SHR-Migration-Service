package org.sharedhealth.migrationservice.feed.encounter;

import com.sun.syndication.feed.atom.Feed;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.migrationservice.client.ShrClient;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.migrationservice.helpers.ResourceHelper.asString;

public class AllEncounterFeedsTest {

    @Mock
    ShrClient shrClient;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldReadResponseAsFeed() throws Exception {
        URI feedUri = URI.create("foo");
        when(shrClient.getFeed(feedUri)).thenReturn(asString("feeds/encounterByCatchmentFeed.xml"));
        AllEncounterFeeds encounterFeeds = new AllEncounterFeeds(shrClient);
        Feed feed = encounterFeeds.getFor(feedUri);
        assertEquals("f3acc815-1a7a-4ec1-a784-992597fcd04c", feed.getId());

    }
}
