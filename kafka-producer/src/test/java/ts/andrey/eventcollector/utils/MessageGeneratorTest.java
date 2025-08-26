package ts.andrey.eventcollector.utils;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import ts.andrey.kafkaproducer.utils.MessageGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageGeneratorTest {

    @Test
    void generateOneDeviceId() {
        //WHEN
        final var result = MessageGenerator.generate(10, 1, true);

        //THEN
        assertEquals(10, result.size());

        result.forEach(it -> {
            assertTrue(StringUtils.isNotBlank(it.getPayload()));
            assertTrue(StringUtils.isNotBlank(it.getEventId()));
            assertNotNull(it.getTimestamp());
            assertNotNull(it.getType());
            final var device = it.getDevice();
            assertNotNull(device);
            assertTrue(StringUtils.isNotBlank(device.getDeviceId()));
            assertTrue(StringUtils.isNotBlank(device.getDeviceType()));
            assertTrue(StringUtils.isNotBlank(device.getMeta()));
            assertNotNull(device.getCreatedAt());
        });

        final var devices = result.stream()
                .map(it -> it.getDevice().getDeviceId())
                .distinct()
                .toList();

        assertEquals(1, devices.size());
    }

    @Test
    void generateManyDeviceId() {
        //WHEN
        final var result = MessageGenerator.generate(10, 10, true);

        //THEN
        assertEquals(10, result.size());

        result.forEach(it -> {
            assertTrue(StringUtils.isNotBlank(it.getPayload()));
            assertTrue(StringUtils.isNotBlank(it.getEventId()));
            assertNotNull(it.getTimestamp());
            assertNotNull(it.getType());
            final var device = it.getDevice();
            assertNotNull(device);
            assertTrue(StringUtils.isNotBlank(device.getDeviceId()));
            assertTrue(StringUtils.isNotBlank(device.getDeviceType()));
            assertTrue(StringUtils.isNotBlank(device.getMeta()));
            assertNotNull(device.getCreatedAt());
        });

        final var devices = result.stream()
                .map(it -> it.getDevice().getDeviceId())
                .distinct()
                .toList();

        assertTrue(devices.size() > 1);
    }

}
