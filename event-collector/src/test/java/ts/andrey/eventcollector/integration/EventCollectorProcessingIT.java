package ts.andrey.eventcollector.integration;

import com.nashkod.avro.Device;
import com.nashkod.avro.EventType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ts.andrey.eventcollector.BaseIntegrationTest;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventKey;
import ts.andrey.eventcollector.tdf.DummyTDF;
import ts.andrey.eventcollector.utils.KafkaConsumerUtil;

import java.util.UUID;
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
                    final var key = new DeviceEventKey();
                    key.setEventId(UUID.fromString(avro.getEventId()));
                    key.setDeviceId(avro.getDeviceId());
                    key.setTimestamp(avro.getTimestamp());
                    DeviceEventEntity eventEntity = null;
                    try {
                        eventEntity = deviceEventDataService.getByEventId(key);
                        final var actualKey = eventEntity.getKey();
                        assertEquals("c9a646d3-9c61-4cb7-b8cd-6f3b5e3d0f7a", actualKey.getEventId().toString());
                        assertEquals("deviceId", actualKey.getDeviceId());
                        assertEquals("10", eventEntity.getPayload());
                        assertEquals("125", String.valueOf(actualKey.getTimestamp()));
                        assertEquals(EventType.TEMPERATURE, eventEntity.getType());
                    } catch (Exception ignored) {
                    } finally {
                        assertNotNull(eventEntity);
                    }
                });

        //THEN CHECK PRODUCE DEVICE ID
        final var kafkaBody = KafkaConsumerUtil.getLastMessage(
                kafka.getBootstrapServers(), "device-id", "device-group",
                schemaRegistry.getFirstMappedPort(), Device.class
        );
        assertEquals("deviceId", kafkaBody.getDeviceId());

        //THEN CHECK IS CACHED
        assertEquals(1, simpleCache.size());
        assertTrue(simpleCache.contains(kafkaBody.getDeviceId()));
    }

}
