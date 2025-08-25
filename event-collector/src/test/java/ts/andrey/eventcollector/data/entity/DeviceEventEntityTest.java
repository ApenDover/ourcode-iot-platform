package ts.andrey.eventcollector.data.entity;

import org.junit.jupiter.api.Test;
import ts.andrey.eventcollector.tdf.DummyTDF;

import static org.junit.jupiter.api.Assertions.*;

class DeviceEventEntityTest {

    @Test
    void testEquals() {
        //GIVEN
        final var one = DummyTDF.deviceEventEntity.getDefault();
        final var two = DummyTDF.deviceEventEntity.getDefault();
        final var list = DummyTDF.deviceEventEntity.getList(2);

        //THEN
        assertEquals(one, two);
        assertEquals(one.getKey(), two.getKey());
        assertNotEquals(list.get(0), list.get(1));
    }

}
