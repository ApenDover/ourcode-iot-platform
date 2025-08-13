package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.CollectorService;
import ts.andrey.eventcollector.service.DeviceEventService;
import ts.andrey.eventcollector.service.DeviceService;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {

    private final DeviceEventService deviceEventService;
    private final DeviceService deviceService;
    private final DeviceEventMapper deviceEventMapper;

    @Override
    public void collect(List<DeviceEvent> events) {
        if (CollectionUtils.isEmpty(events)) {
            return;
        }

        final var correctEvents = events.stream()
                .filter(Objects::nonNull)
                .filter(it -> {
                    final var isValid = StringUtils.isNotEmpty(it.getDeviceId())
                            && StringUtils.isNotEmpty(it.getEventId())
                            && StringUtils.isNotEmpty(it.getPayload())
                            && Objects.nonNull(it.getType())
                            && it.getTimestamp() > 0;
                    if (!isValid) {
                        log.warn("Невалидное событие: {}", it);
                    }
                    return isValid;
                }).toList();

        if (CollectionUtils.isEmpty(correctEvents)) {
            return;
        }

        try {
            final var unsavedDeviceEvent = deviceEventService.saveCashedEvents(correctEvents);
            final var uncachedDevices = deviceEventMapper.toDeviceIdList(unsavedDeviceEvent);
            deviceService.process(uncachedDevices);
            deviceEventService.saveEvents(unsavedDeviceEvent);
        } catch (Exception e) {
            log.error("Ошибка при обработке событий", e);
        }
    }

}
