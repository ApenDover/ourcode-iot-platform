package ts.andrey.eventservice.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.eventservice.model.EventFilterRequest;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyEventFilterRequest {

    public EventFilterRequest getDefault() {
        return EventFilterRequest.builder()
                .size(10)
                .page(2)
                .type("type")
                .fromTimestamp(100L)
                .toTimestamp(1000L)
                .build();
    }

    public EventFilterRequest getDefault(Integer size, Integer page) {
        return EventFilterRequest.builder()
                .size(size)
                .page(page)
                .type("type")
                .fromTimestamp(100L)
                .toTimestamp(1000L)
                .build();
    }

}
