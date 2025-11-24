package ts.andrey.deviceservice.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ts.andrey.deviceservice.tdf.DummyTDF;
import ts.andrey.dto.DeviceStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeviceMapperTest {

    DeviceMapper deviceMapper = Mappers.getMapper(DeviceMapper.class);

    @Test
    void testMapToUpdateVersionResponse() {
        //GIVEN
        final var device = DummyTDF.deviceEntity.getDefault();
        final var oldVersion = "1.1.1";

        //WHEN
        final var actual = deviceMapper.toUpdateVersionResponse(device, oldVersion);

        //THEN
        assertEquals("deviceId",actual.getDeviceId());
        assertEquals("1.1.1",actual.getPrevVersion());
        assertEquals("0.0.1",actual.getTargetVersion());
        assertEquals(DeviceStatus.READY,actual.getStatus());
        assertEquals(1L,actual.getEtag());
    }

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
        assertEquals("1970-01-01T00:00:00.600Z", actual.getCreatedAt().toString());
        assertEquals("meta", actual.getMeta());
        assertEquals(1L, actual.getEtag());
        assertEquals(DeviceStatus.READY, actual.getStatus());
    }

    @Test
    void toEntityWithNullCreatedAt() {
        // GIVEN
        final var device = DummyTDF.device.getDefault();
        device.setCreatedAt(null);

        // WHEN
        final var actual = deviceMapper.toEntity(device);

        // THEN
        assertNotNull(actual.getId());
        assertEquals("deviceId", actual.getDeviceId());
        assertEquals("deviceType", actual.getDeviceType());
        assertNull(actual.getCreatedAt());
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

    @Test
    void fromRequestToEntity() {
        // GIVEN
        final var request = DummyTDF.deviceCreateRequest.getDefault();

        // WHEN
        final var actual = deviceMapper.createDevice(request);

        // THEN
        assertNotNull(actual.getDeviceId());
        assertNull(actual.getCreatedAt());
        assertEquals("deviceType", actual.getDeviceType());
        assertEquals("meta", actual.getMeta());
    }

}
