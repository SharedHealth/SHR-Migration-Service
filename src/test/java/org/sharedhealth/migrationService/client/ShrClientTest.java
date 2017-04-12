package org.sharedhealth.migrationService.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.migrationService.config.SHRMigrationProperties;
import org.sharedhealth.migrationService.identity.IdentityToken;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.migrationService.helpers.ResourceHelper.asString;
import static org.sharedhealth.migrationService.utils.Headers.*;

public class ShrClientTest {

    @Mock
    IdentityServiceClient identityServiceClient;

    @Mock
    private SHRMigrationProperties properties;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);
    private ShrClient shrClient;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        shrClient = new ShrClient(identityServiceClient, properties);
    }

    @Test
    public void shouldGetResponse() throws Exception {
        String clientId = "12345";
        String clientEmail = "email@gmail.com";
        String body = asString("feeds/encounterByCatchmentFeed.xml");

        when(identityServiceClient.getOrCreateToken()).thenReturn(new IdentityToken("baz"));
        when(properties.getIdpClientId()).thenReturn(clientId);
        when(properties.getIdpClientEmail()).thenReturn(clientEmail);

        givenThat(get(urlPathEqualTo("/catchments/3026/encounters"))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo("baz"))
                .withHeader(CLIENT_ID_KEY, equalTo(clientId))
                .withHeader(FROM_KEY, equalTo(clientEmail))
                .withHeader("Accept", equalTo("application/atom+xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(body)));

        String response = shrClient.getFeed(URI.create("http://localhost:9997/catchments/3026/encounters"));
        assertNotNull(response);
        assertEquals(response, body);
    }

    @Test
    public void shouldClearIdentityTokenIfUnauthorized() throws Exception {
        String clientId = "12345";
        String clientEmail = "email@gmail.com";

        when(identityServiceClient.getOrCreateToken()).thenReturn(new IdentityToken("baz"));
        when(properties.getIdpClientId()).thenReturn(clientId);
        when(properties.getIdpClientEmail()).thenReturn(clientEmail);

        givenThat(get(urlPathEqualTo("/catchments/3026/encounters"))
                .withHeader("Accept", equalTo("application/atom+xml"))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo("baz"))
                .withHeader(CLIENT_ID_KEY, equalTo(clientId))
                .withHeader(FROM_KEY, equalTo(clientEmail))
                .willReturn(aResponse()
                        .withStatus(401)));

        shrClient.getFeed(URI.create
                ("http://localhost:9997/catchments/3026/encounters"));

        verify(identityServiceClient, times(1)).clearToken();
    }


}
