package ts.andrey.eventcollector.integration;

import com.nashkod.avro.Device;
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
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    final var events = deviceEventRepository.findAll();
                    assertEquals(size, events.size());
                });

        //THEN CHECK PRODUCE DEVICE ID
        final var kafkaBody = KafkaConsumerUtil.getLastMessage(
                kafka.getBootstrapServers(), "device-id", "device-group",
                schemaRegistry.getFirstMappedPort(), Device.class
        );
        assertEquals("deviceId-0", kafkaBody.getDeviceId());

        //THEN CHECK IS CACHED
        assertEquals(10, simpleCache.size());
        assertTrue(simpleCache.contains(kafkaBody.getDeviceId()));
    }

}
