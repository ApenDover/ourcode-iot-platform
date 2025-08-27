package ts.andrey.deviceservice;

import org.junit.jupiter.api.Test;
import ts.andrey.deviceservice.exception.TextException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextExceptionTest {

    @Test
    void test() {
        final var actual = String.format(TextException.DEVICE_NOT_FOUND.getDescription(), "deviceId");
        assertEquals("Устройство с deviceId [deviceId] не найдено", actual);
    }

}
