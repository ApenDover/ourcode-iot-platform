package ts.andrey.eventcollector.integration;

import com.nashkod.avro.Device;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ts.andrey.eventcollector.BaseIntegrationTest;
import ts.andrey.eventcollector.tdf.DummyTDF;
import ts.andrey.eventcollector.testutils.KafkaConsumerUtil;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCollectorProcessingIT extends BaseIntegrationTest {

    @Test
    @SneakyThrows
    void testSendMessageSuccessCaseWithBatch() {
        // GIVEN
        final var size = 10;
        final var deviceEvents = DummyTDF.deviceEvent.getList(size);

        // WHEN
        final var metadata = producer.send(deviceEvents).get();

        // THEN CHECK PRODUCE METADATA
        assertNotNull(metadata);
        assertEquals("events", metadata.get(1).topic());
        assertTrue(metadata.get(1).offset() >= 0);

        //THEN CHECK CASSANDRA SAVED
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    final var events = deviceEventRepository.findAll();
                    assertEquals(size, events.size());
                });

        //THEN CHECK PRODUCE DEVICE ID
        final var kafkaRecords = KafkaConsumerUtil.getLastMessage(
                kafka.getBootstrapServers(), "device", "device-group",
                schemaRegistry.getFirstMappedPort(), Device.class
        );
        kafkaRecords.forEach(r -> {
            final var device = r.value();
            assertTrue(device.getDeviceId().contains("deviceId-"));
            assertEquals(Instant.ofEpochMilli(300L), device.getCreatedAt());
            assertEquals("meta", device.getMeta());
            assertEquals("deviceType", device.getDeviceType());
            assertTrue(simpleCache.contains(device.getDeviceId()));
        });

        //THEN CHECK IT CACHED
        assertEquals(10, simpleCache.size());
    }

}
