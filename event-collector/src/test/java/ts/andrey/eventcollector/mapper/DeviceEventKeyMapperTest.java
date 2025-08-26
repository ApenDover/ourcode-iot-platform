package ts.andrey.eventcollector.mapper;

import com.nashkod.avro.EventType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ts.andrey.eventcollector.tdf.DummyTDF;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceEventKeyMapperTest {

    private final DeviceEventMapper deviceEventMapper = Mappers.getMapper(DeviceEventMapper.class);

    @Test
    void toEntity() {
        //GIVEN
        final var deviceEvent = DummyTDF.deviceEvent.getDefault();

        //WHEN
        final var actual = deviceEventMapper.eventToEntity(deviceEvent);

        //THEN
        assertNotNull(actual);
        assertEquals("c9a646d3-9c61-4cb7-b8cd-6f3b5e3d0f7a", actual.getKey().getEventId().toString());
        assertEquals("deviceId", actual.getKey().getDeviceId());
        assertEquals(125L, actual.getKey().getTimestamp());
        assertEquals(EventType.TEMPERATURE, actual.getType());
        assertEquals("10", actual.getPayload());
    }

}
