package ts.andrey.deviceservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TextException {

    DEVICE_NOT_FOUND("Устройство с deviceId [%s] не найдено");

    @Getter
    private final String description;

    public String format(Object... args) {
        return String.format(description, args);
    }

}
