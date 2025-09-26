package ts.andrey.eventservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorExceptionMessages {

    EVENT_NOT_FOUND("Событие с deviceId [%s] и eventId [%s] не найдено", HttpStatus.NOT_FOUND);

    private final String description;
    private final HttpStatus httpStatus;

    public String format(Object... args) {
        return String.format(description, args);
    }

}
