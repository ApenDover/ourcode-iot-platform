package ts.andrey.eventcollector.integration;

import com.nashkod.avro.DeviceId;
import com.nashkod.avro.EventType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ts.andrey.eventcollector.BaseIntegrationTest;
import ts.andrey.eventcollector.tdf.DummyTDF;
import ts.andrey.eventcollector.utils.KafkaConsumerUtil;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCollectorProcessingIT extends BaseIntegrationTest {

    @Test
    @SneakyThrows
    void testSendMessageSuccessCase() {
        // GIVEN
        final var avro = DummyTDF.deviceEvent.getDefault();

        // WHEN
        final var metadata = producer.sendEvent(avro).get();

        // THEN CHECK PRODUCE METADATA
        assertNotNull(metadata);
        assertEquals("events", metadata.topic());
        assertTrue(metadata.offset() >= 0);

        //THEN CHECK CASSANDRA SAVED
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var eventEntity = deviceEventDataService.getByEventId(avro.getEventId());
                    assertEquals("eventId", eventEntity.getEventId());
                    assertEquals("deviceId", eventEntity.getDeviceId());
                    assertEquals("10", eventEntity.getPayload());
                    assertEquals("125", eventEntity.getTimestamp().toString());
                    assertEquals(EventType.TEMPERATURE, eventEntity.getType());
                });

        //THEN CHECK PRODUCE DEVICE ID
        final var kafkaBody = KafkaConsumerUtil.getLastMessage(
                kafka.getBootstrapServers(), "device-id", "device-group",
                schemaRegistry.getFirstMappedPort(), DeviceId.class
        );
        assertEquals("deviceId", kafkaBody.getDeviceId());
    }

}
