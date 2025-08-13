package ts.andrey.eventcollector.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DeviceEventValidFilterTest {

    @Test
    void getCorrectSuccessCase() {
        //GIVEN
        final var validEvent = DummyTDF.deviceEvent.getDefault();
        final var invalidEvent = DummyTDF.deviceEvent.getInvalid();

        //WHEN
        final var actual = DeviceEventValidFilter.getCorrect(List.of(validEvent, invalidEvent));

        //THEN
        assertEquals(1, actual.size());
        assertEquals(validEvent, actual.get(0));
    }

    @Test
    void getCorrectEmptyCase() {
        //WHEN
        final var actual = Assertions.assertDoesNotThrow(
                () -> DeviceEventValidFilter.getCorrect(List.of())
        );

        //THEN
        assertTrue(actual.isEmpty());
    }

    @Test
    void getCorrectNullCase() {
        //WHEN
        final var actual = Assertions.assertDoesNotThrow(
                () -> DeviceEventValidFilter.getCorrect(null)
        );

        //THEN
        assertTrue(actual.isEmpty());
    }

}
