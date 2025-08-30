package ts.andrey.deviceservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum TextException {

    DEVICE_NOT_FOUND("Устройство с deviceId [%s] не найдено", HttpStatus.NOT_FOUND),
    REDIS_NOT_AVAILABLE("REDIS недоступен: %s", HttpStatus.SERVICE_UNAVAILABLE);

    @Getter
    private final String description;
    @Getter
    private final HttpStatus httpStatus;

    public String format(Object... args) {
        return String.format(description, args);
    }

}
