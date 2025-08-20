package ts.andrey.eventcollector.service.component;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.annotation.WithSpan;
import ts.andrey.eventcollector.service.DeviceEventService;
import ts.andrey.eventcollector.service.DeviceService;
import ts.andrey.eventcollector.validation.DeviceEventValidFilter;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorFacade {

    private final DeviceEventService deviceEventService;
    private final DeviceService deviceService;

    @WithSpan("event-collector-processing")
    public void collect(List<DeviceEvent> events) {
        try {
            final var deviceEvents = DeviceEventValidFilter.getCorrect(events);

            if (CollectionUtils.isEmpty(deviceEvents)) {
                return;
            }

            deviceService.sendUniqueDeviceids(deviceEvents);
            deviceEventService.saveEvents(deviceEvents);

        } catch (Exception e) {
            log.error("Ошибка при обработке событий", e);
        }
    }

}
