package ts.andrey.kafkaproducer.model;

import com.nashkod.avro.DeviceEvent;
import io.opentelemetry.api.trace.Span;
import lombok.Getter;

import java.util.List;

@Getter
public class GenerateResponse {

    public GenerateResponse(List<DeviceEvent> events, Integer deviceCount, Integer messageCount) {
        this.messageCount = messageCount;
        this.deviceCount = deviceCount;
        deviceIds = events.stream()
                .map(it -> it.getDevice().getDeviceId())
                .distinct()
                .toList();
        trace = Span.current().getSpanContext().getTraceId();
    }

    private final Integer messageCount;
    private final Integer deviceCount;
    private final List<String> deviceIds;
    private final String trace;

}
