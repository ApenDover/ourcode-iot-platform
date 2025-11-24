package ts.andrey.eventservice.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ts.andrey.eventservice.tdf.DummyTDF;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventMapperTest {

    EventMapper eventMapper = Mappers.getMapper(EventMapper.class);

    @Test
    void entityToEvent() {
        //GIVEN
        final var entity = DummyTDF.deviceEventEntity.getDefault();

        //WHEN
        final var actual = eventMapper.entityToEvent(entity);

        //THEN
        assertNotNull(actual);
        assertEquals("892dd1da-6f3f-49bc-a60d-a2b282d6efd0", actual.getEventId());
        assertEquals("deviceIdKey", actual.getDeviceId());
        assertEquals(897L, actual.getTimestamp());
        assertEquals("TEMPERATURE", actual.getType());
        assertEquals("payload", actual.getPayload());
    }

    @Test
    void entityToPage() {
        //GIVEN
        final var entities = DummyTDF.deviceEventEntity.getList(10);
        final var filter = DummyTDF.eventFilterRequest.getDefault(10, 1);

        //WHEN
        final var actual = eventMapper.entityListToEventPage(entities, filter, entities.size());

        //THEN
        assertNotNull(actual);
        assertEquals(10, actual.getEvents().size());
        assertEquals(1, actual.getPage());
        assertEquals(10, actual.getSize());
        assertEquals(10, actual.getTotal());
    }

}
