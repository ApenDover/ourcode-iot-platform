package ts.andrey.deviceservice;

import org.junit.jupiter.api.Test;
import ts.andrey.deviceservice.exception.ErrorExceptionMessages;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorExceptionMessagesTest {

    @Test
    void test() {
        final var actual = String.format(ErrorExceptionMessages.DEVICE_NOT_FOUND.getDescription(), "deviceId");
        assertEquals("Устройство с deviceId [deviceId] не найдено", actual);
    }

}
