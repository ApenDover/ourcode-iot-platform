package ts.andrey.deviceservice.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ts.andrey.deviceservice.tdf.DummyTDF;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceMapperTest {

    DeviceMapper deviceMapper = Mappers.getMapper(DeviceMapper.class);

    @Test
    void toEntity() {
        // GIVEN
        final var device = DummyTDF.device.getDefault();

        // WHEN
        final var actual = deviceMapper.toEntity(device);

        // THEN
        assertNotNull(actual.getId());
        assertEquals("deviceId", actual.getDeviceId());
        assertEquals("deviceType", actual.getDeviceType());
        assertEquals("1970-01-01T00:10:00Z", actual.getCreatedAt().toString());
        assertEquals("meta", actual.getMeta());
    }

    @Test
    void toDevice() {
        // GIVEN
        final var device = DummyTDF.deviceEntity.getDefault();

        // WHEN
        final var actual = deviceMapper.toDevice(device);

        // THEN
        assertEquals("deviceId", actual.getDeviceId());
        assertEquals("deviceType", actual.getDeviceType());
        assertEquals(200L, actual.getCreatedAt());
        assertEquals("meta", actual.getMeta());
    }

}
