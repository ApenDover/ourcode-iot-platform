package ts.andrey.eventcollector.integration;

import com.nashkod.avro.EventType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ts.andrey.eventcollector.BaseIntegrationTest;
import ts.andrey.eventcollector.TDF.DummyTDF;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaProducerConsumerIT extends BaseIntegrationTest {

    @Test
    @SneakyThrows
    void testSendMessage() {
        //GIVEN
        final var avro = DummyTDF.deviceEvent.getDefault();

        //WHEN
        final var metadata = producer.sendEvent(avro).get();

        //THEN
        assertNotNull(metadata);
        assertEquals("events", metadata.topic());
        assertTrue(metadata.offset() >= 0);

        Thread.sleep(2000);

        final var eventEntity = deviceEventService.getByEventId(avro.getEventId());
        assertEquals("eventId", eventEntity.getEventId());
        assertEquals("deviceId", eventEntity.getDeviceId());
        assertEquals("10", eventEntity.getPayload());
        assertEquals("125", eventEntity.getTimestamp().toString());
        assertEquals(EventType.TEMPERATURE, eventEntity.getType());
    }

}
