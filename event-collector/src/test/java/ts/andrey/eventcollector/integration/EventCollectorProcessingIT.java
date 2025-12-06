package ts.andrey.eventcollector.integration;

import com.nashkod.avro.Device;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.BaseIntegrationTest;
import ts.andrey.eventcollector.tdf.DummyTDF;
import ts.andrey.eventcollector.testutils.KafkaConsumerUtil;
import ts.andrey.eventcollector.testutils.KafkaProducerUtil;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCollectorProcessingIT extends BaseIntegrationTest {

    @Test
    @SneakyThrows
    void testSendMessageSuccessCaseWithBatch() {
        // GIVEN
        final var size = 10;
        final var deviceEvents = DummyTDF.deviceEvent.getList(size);

        // WHEN
        final var metadata = KafkaProducerUtil.sendMessages(
                kafka.getBootstrapServers(), "events", schemaRegistry.getFirstMappedPort(), deviceEvents
        );


        // THEN CHECK PRODUCE METADATA
        assertFalse(CollectionUtils.isEmpty(metadata));

        //THEN CHECK CASSANDRA SAVED
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    final var events = deviceEventRepository.findAll();
                    assertEquals(size, events.size());
                });

        //THEN CHECK PRODUCE DEVICE ID
        final var kafkaRecords = KafkaConsumerUtil.getMessages(
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
        KafkaProducerUtil.cleanup();
    }

}
