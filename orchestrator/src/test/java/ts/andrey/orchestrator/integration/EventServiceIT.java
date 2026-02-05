package ts.andrey.orchestrator.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

public class EventServiceIT extends BaseIntegrationTest {

    @Test
    void timeOutTest() {
        //GIVEN
        stubFor(get(urlEqualTo("/api/v1/events/9814a2c6-4641-4bdc-b4b7-e724969b2f17?device_id=01K6GJ564FPTXDWX8R1F91VZK0"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withFixedDelay(10000)
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Delayed response\"}")));

        final var actual = sendRequest("/api/v1/events/9814a2c6-4641-4bdc-b4b7-e724969b2f17?device_id=01K6GJ564FPTXDWX8R1F91VZK0",
                HttpMethod.GET, null, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertNotNull(actual.getBody());
        final var body = actual.getBody().toLowerCase();
        assertTrue(body.contains("timed out") || body.contains("timeout"));
    }

}
