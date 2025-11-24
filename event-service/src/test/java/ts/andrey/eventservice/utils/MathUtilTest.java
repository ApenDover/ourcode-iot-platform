package ts.andrey.eventservice.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MathUtilTest {

    @ParameterizedTest
    @CsvSource({
            "10, 3, 4",
            "9, 3, 3",
            "1, 2, 1",
            "0, 5, 0",
            "12, 200, 1"
    })
    void divideCeiling(Integer a, Integer b, Integer result) {
        final var actual = MathUtil.divideCeiling(a, b);
        assertEquals(result, actual);
    }

}
