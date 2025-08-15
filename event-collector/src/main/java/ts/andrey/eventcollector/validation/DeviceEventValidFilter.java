package ts.andrey.eventcollector.validation;

import com.nashkod.avro.DeviceEvent;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@UtilityClass
public class DeviceEventValidFilter {

    public List<DeviceEvent> getCorrect(List<DeviceEvent> events) {
        if (CollectionUtils.isEmpty(events)) {
            return List.of();
        }
        return events.stream()
                .filter(Objects::nonNull)
                .filter(it -> {
                    final var isValid = StringUtils.isNotEmpty(it.getDevice().getDeviceId())
                            && StringUtils.isNotEmpty(it.getEventId())
                            && StringUtils.isNotEmpty(it.getPayload())
                            && Objects.nonNull(it.getType())
                            && it.getTimestamp() > 0;
                    if (!isValid) {
                        log.warn("Невалидное событие: {}", it);
                    }
                    return isValid;
                }).toList();
    }

}
