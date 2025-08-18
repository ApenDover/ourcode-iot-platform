package ts.andrey.devicecollector.integration;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ts.andrey.devicecollector.BaseIntegrationTest;
import ts.andrey.devicecollector.tdf.DummyTDF;
import ts.andrey.devicecollector.testutils.KafkaProducerUtil;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeviceCollectorProcessingIT extends BaseIntegrationTest {

    @Test
    @SneakyThrows
    void testSendMessageSuccessCaseWithBatch() {
        // GIVEN
        final var device = DummyTDF.device.getDefault();

        // WHEN
        KafkaProducerUtil.sendMessage(
                kafka.getBootstrapServers(),
                "device",
                schemaRegistry.getFirstMappedPort(),
                device
        );

        // THEN
//        await().atMost(10, TimeUnit.SECONDS)
//                .pollInterval(1000, MILLISECONDS)
//                .untilAsserted(() -> {
//                    final var result = assertDoesNotThrow(() -> deviceRepository.findAll());
//                    assertFalse(result.isEmpty());
//                    assertEquals(1, result.size());
//                    final var entity = result.get(0);
//                    assertEquals(26, entity.getDeviceId().length());
//                    assertEquals("deviceId", entity.getDeviceId().trim());
//                    assertEquals("deviceType", entity.getDeviceType());
//                    assertEquals("meta", entity.getMeta());
//                    assertEquals(Instant.ofEpochMilli(300L), entity.getCreatedAt());
//                    assertEquals(1,entity.getVersion());
//                });

        Thread.sleep(5000);

        final var result = assertDoesNotThrow(() -> deviceRepository.findAll());
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        final var entity = result.get(0);
        assertEquals(26, entity.getDeviceId().length());
        assertEquals("deviceId", entity.getDeviceId().trim());
        assertEquals("deviceType", entity.getDeviceType());
        assertEquals("meta", entity.getMeta());
        assertEquals(Instant.ofEpochMilli(300L), entity.getCreatedAt());
        assertEquals(1, entity.getVersion());

        //GIVEN
        final var updateDevice = DummyTDF.device.getDefaultWithOtherMeta();

        // WHEN
        KafkaProducerUtil.sendMessage(
                kafka.getBootstrapServers(),
                "device",
                schemaRegistry.getFirstMappedPort(),
                updateDevice
        );

        // THEN
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1000, MILLISECONDS)
                .untilAsserted(() -> {
                    final var resultUpdate = assertDoesNotThrow(() -> deviceRepository.findAll());
                    assertFalse(resultUpdate.isEmpty());
                    assertEquals(1, resultUpdate.size());
                    final var entityUpdate = resultUpdate.get(0);
                    assertEquals(26, entityUpdate.getDeviceId().length());
                    assertEquals("deviceId", entityUpdate.getDeviceId().trim());
                    assertEquals("deviceType", entityUpdate.getDeviceType());
                    assertEquals("updated", entityUpdate.getMeta());
                    assertEquals(Instant.ofEpochMilli(600L), entityUpdate.getCreatedAt());
                    assertEquals(2, entityUpdate.getVersion());
                });
    }

}
