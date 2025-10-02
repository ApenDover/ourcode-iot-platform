package ts.andrey.eventservice.integration;

import org.junit.jupiter.api.Test;
import ts.andrey.dto.Event;
import ts.andrey.eventservice.BaseIntegrationTest;
import ts.andrey.eventservice.model.ResponseError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventServiceIT extends BaseIntegrationTest {

    @Test
    void testGetOneEvent() {
        //GIVEN
        final var eventId = "550e8400-e29b-41d4-a716-446655440000";
        // WHEN
        final var result = sendGet("/api/v1/events/"
                + eventId + "?device_id=device-1", Event.class);
        final var body = result.getBody();

        //THEN
        assertNotNull(result);
        assertNotNull(body);
        assertNotNull(body.getTimestamp());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", body.getEventId());
        assertEquals("device-1", body.getDeviceId());
        assertEquals("TEMPERATURE", body.getType());
        assertEquals("{\"temp\":22.5,\"unit\":\"C\"}", body.getPayload());
    }

    @Test
    void testGetOneNotFound() {
        // WHEN
        //GIVEN
        final var eventId = "150e8400-e29b-41d4-a716-446655440000";
        // WHEN
        final var result = sendGet("/api/v1/events/"
                + eventId + "?device_id=device-1", ResponseError.class);
        final var body = result.getBody();

        //THEN
        assertNotNull(result);
        assertEquals("Entity not found", body.getTitle());
        assertEquals(404, body.getStatus());
        assertEquals("Событие с deviceId [device-1] "
                + "и eventId [150e8400-e29b-41d4-a716-446655440000] не найдено", body.getDetail());
        assertEquals("/api/v1/events/150e8400-e29b-41d4-a716-446655440000", body.getInstance());
        assertNotNull(body.getTrace());
    }

}
