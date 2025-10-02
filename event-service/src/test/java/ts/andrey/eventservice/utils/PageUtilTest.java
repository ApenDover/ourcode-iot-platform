package ts.andrey.eventservice.utils;

import org.junit.jupiter.api.Test;
import ts.andrey.eventservice.tdf.DummyTDF;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageUtilTest {

    @Test
    void getPageableListSuccess() {
        //GIVEN
        final var entities = DummyTDF.deviceEventEntity.getList(53);
        final var filter = DummyTDF.eventFilterRequest.getDefault(10, 2);

        //WHEN
        final var actual = PageUtil.getPageableList(entities, filter);

        //THEN
        assertEquals(10, actual.size());
        assertEquals("payload-10", actual.get(0).getPayload());
        assertEquals("payload-19", actual.get(9).getPayload());
    }

    @Test
    void getPageableShortList() {
        //GIVEN
        final var entities = DummyTDF.deviceEventEntity.getList(3);
        final var filter = DummyTDF.eventFilterRequest.getDefault(10, 2);

        //WHEN
        final var actual = PageUtil.getPageableList(entities, filter);

        //THEN
        assertEquals(3, actual.size());
        assertEquals("payload-0", actual.get(0).getPayload());
        assertEquals("payload-2", actual.get(2).getPayload());
    }

    @Test
    void getPageableEmptyPage() {
        //GIVEN
        final var entities = DummyTDF.deviceEventEntity.getList(30);
        final var filter = DummyTDF.eventFilterRequest.getDefault(10, null);

        //WHEN
        final var actual = PageUtil.getPageableList(entities, filter);

        //THEN
        assertEquals(10, actual.size());
        assertEquals("payload-0", actual.get(0).getPayload());
        assertEquals("payload-9", actual.get(9).getPayload());
    }

    @Test
    void getPageableShortListAndBigRequest() {
        //GIVEN
        final var entities = DummyTDF.deviceEventEntity.getList(5);
        final var filter = DummyTDF.eventFilterRequest.getDefault(10, 2);

        //WHEN
        final var actual = PageUtil.getPageableList(entities, filter);

        //THEN
        assertEquals(5, actual.size());
        assertEquals("payload-0", actual.get(0).getPayload());
        assertEquals("payload-4", actual.get(4).getPayload());
    }

    @Test
    void getPageableRequestSizeIsNull() {
        //GIVEN
        final var entities = DummyTDF.deviceEventEntity.getList(5);
        final var filter = DummyTDF.eventFilterRequest.getDefault(null, 2);

        //WHEN
        final var actual = PageUtil.getPageableList(entities, filter);

        //THEN
        assertEquals(5, actual.size());
        assertEquals("payload-0", actual.get(0).getPayload());
        assertEquals("payload-4", actual.get(4).getPayload());
    }

    @Test
    void getPageableRequestPageIsOversize() {
        //GIVEN
        final var entities = DummyTDF.deviceEventEntity.getList(30);
        final var filter = DummyTDF.eventFilterRequest.getDefault(10, 9);

        //WHEN
        final var actual = PageUtil.getPageableList(entities, filter);

        //THEN
        assertEquals(10, actual.size());
        assertEquals("payload-20", actual.get(0).getPayload());
        assertEquals("payload-29", actual.get(9).getPayload());
    }

    @Test
    void getPageableRequestSizeZero() {
        //GIVEN
        final var entities = DummyTDF.deviceEventEntity.getList(2);
        final var filter = DummyTDF.eventFilterRequest.getDefault(0, 2);

        //WHEN
        final var actual = PageUtil.getPageableList(entities, filter);

        //THEN
        assertEquals(2, actual.size());
        assertEquals("payload-0", actual.get(0).getPayload());
        assertEquals("payload-1", actual.get(1).getPayload());
    }


}
