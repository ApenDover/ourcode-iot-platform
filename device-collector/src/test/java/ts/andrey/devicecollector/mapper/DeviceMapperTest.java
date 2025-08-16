package ts.andrey.devicecollector.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ts.andrey.devicecollector.tdf.DummyTDF;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceMapperTest {

    DeviceMapper deviceMapper = Mappers.getMapper(DeviceMapper.class);

    @Test
    void toDeviceEntity() {
        //GIVEN
        final var device = DummyTDF.device.getDefault();

        //WHEN
        final var deviceEntity = deviceMapper.toDeviceEntity(device);

        //THEN
        assertEquals("deviceId", deviceEntity.getDeviceId());
        assertEquals("deviceType", deviceEntity.getDeviceType());
        assertEquals(300L, deviceEntity.getCreatedAt());
        assertEquals("meta", deviceEntity.getMeta());
    }

    @Test
    void toDevice() {
        //GIVEN
        final var deviceEntity = DummyTDF.deviceEntity.getDefault();

        //WHEN
        final var device = deviceMapper.toDevice(deviceEntity);

        //THEN
        assertEquals("deviceId", device.getDeviceId());
        assertEquals("deviceType", device.getDeviceType());
        assertEquals(300L, device.getCreatedAt());
        assertEquals("meta", device.getMeta());
    }

}
