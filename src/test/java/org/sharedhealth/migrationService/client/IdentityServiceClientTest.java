package org.sharedhealth.migrationService.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sharedhealth.migrationService.config.SHRMigrationProperties;
import org.sharedhealth.migrationService.identity.IdentityStore;
import org.sharedhealth.migrationService.identity.IdentityToken;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.sharedhealth.migrationService.utils.Headers.CLIENT_ID_KEY;
import static org.sharedhealth.migrationService.utils.Headers.X_AUTH_TOKEN_KEY;

public class IdentityServiceClientTest {

    private SHRMigrationProperties properties;
    private IdentityStore identityStore;
    private IdentityServiceClient identityServiceClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);

    @Before
    public void setUp() throws Exception {
        properties = mock(SHRMigrationProperties.class);
        identityStore = mock(IdentityStore.class);
        identityServiceClient = new IdentityServiceClient(properties, identityStore);
    }

    @Test
    public void shouldGetTheTokenFromIdentityStore() throws Exception {
        IdentityToken identityToken = new IdentityToken("xyz");
        when(identityStore.getToken()).thenReturn(identityToken);
        IdentityToken token = identityServiceClient.getOrCreateToken();
        assertEquals(token, identityToken);
        verify(identityStore, never()).setToken(any(IdentityToken.class));
    }

    @Test
    public void shouldCreateTheTokenWhenNotFoundInIdentityStore() throws Exception {
        String clientId = "12345";
        String clientAuthToken = "abc";
        IdentityToken identityToken = new IdentityToken("xyz");
        String response = "{\"access_token\" : \"" + identityToken.toString() + "\"}";

        when(identityStore.getToken()).thenReturn(null);
        when(properties.getIdpServerLoginUrl()).thenReturn("http://localhost:9997/signin");
        when(properties.getIdpClientAuthToken()).thenReturn(clientAuthToken);
        when(properties.getIdpClientId()).thenReturn(clientId);

        givenThat(post(urlEqualTo("/signin"))
                .withHeader("accept", equalTo("application/json"))
                .withHeader(CLIENT_ID_KEY, equalTo(clientId))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(clientAuthToken))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(response)
                )
        );

        IdentityToken token = identityServiceClient.getOrCreateToken();
        assertEquals(token.toString(), identityToken.toString());
        verify(identityStore, times(1)).setToken(token);
    }


}
