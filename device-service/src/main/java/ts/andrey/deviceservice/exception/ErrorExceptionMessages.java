package ts.andrey.deviceservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorExceptionMessages {

    DEVICE_NOT_FOUND("Устройство с deviceId [%s] не найдено", HttpStatus.NOT_FOUND),
    REDIS_NOT_AVAILABLE("REDIS недоступен: %s", HttpStatus.SERVICE_UNAVAILABLE);

    private final String description;
    private final HttpStatus httpStatus;

    public String format(Object... args) {
        return String.format(description, args);
    }

}
