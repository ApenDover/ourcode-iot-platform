package ts.andrey.devicecollector.integration;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ts.andrey.devicecollector.BaseIntegrationTest;
import ts.andrey.devicecollector.tdf.DummyTDF;
import ts.andrey.devicecollector.testutils.KafkaProducerUtil;

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
        final var sendResult = KafkaProducerUtil.sendMessage(
                kafka.getBootstrapServers(),
                "device",
                schemaRegistry.getFirstMappedPort(),
                device
        );

        // THEN
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1000, MILLISECONDS)
                .untilAsserted(() -> {
                    final var result = assertDoesNotThrow(() -> deviceRepository.findAll());
                    assertFalse(result.isEmpty());
                    final var entity = result.get(0);
                    assertEquals(26, entity.getDeviceId().length());
                    assertEquals("deviceId", entity.getDeviceId().trim());
                    assertEquals("deviceType", entity.getDeviceType());
                    assertEquals("meta", entity.getMeta());
                    assertEquals(300L, entity.getCreatedAt());
                });
    }

}
