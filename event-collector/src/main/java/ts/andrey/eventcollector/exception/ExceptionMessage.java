package ts.andrey.eventcollector.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ExceptionMessage {

    CASSANDRA_DEVICE_EVENT_NOT_FOUND("device event not found with %s = %s"),
    UNRECOGNIZED_RECORD_TYPE("Record type is not DeviceEvent");

    @Getter
    private final String value;

    @Override
    public String toString() {
        return value;
    }

}
