package ts.andrey.eventcollector.service.component;

import com.nashkod.avro.DeviceEvent;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.metrics.EventCollectorMetrics;
import ts.andrey.eventcollector.service.EventService;
import ts.andrey.eventcollector.service.DeviceService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorFacade {

    private final EventService eventService;
    private final DeviceService deviceService;
    private final EventCollectorMetrics eventCollectorMetrics;

    @Observed(name = "event-collector-processing")
    public void collect(List<DeviceEvent> events) {
        try {
            if (CollectionUtils.isEmpty(events)) {
                return;
            }
            deviceService.sendUniqueDeviceids(events);
            eventService.saveEvents(events);
            eventCollectorMetrics.incrementSuccess();
        } catch (Exception e) {
            eventCollectorMetrics.incrementError();
            log.error("Ошибка при обработке событий", e);
            throw e;
        }
    }

}
