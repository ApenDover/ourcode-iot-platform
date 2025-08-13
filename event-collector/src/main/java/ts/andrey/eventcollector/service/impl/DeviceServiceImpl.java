package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.DeduplicateService;
import ts.andrey.eventcollector.service.DeviceEventProducer;
import ts.andrey.eventcollector.service.DeviceService;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeduplicateService deduplicateService;
    private final DeviceEventProducer deviceIdProducerImpl;
    private final DeviceEventMapper deviceEventMapper;

    /**
     * Отправляем в топик новые deviceId
     *
     * @param deviceEvents список событий
     */
    public void process(List<DeviceEvent> deviceEvents) {
        if (CollectionUtils.isEmpty(deviceEvents)) {
            return;
        }
        final var uniqEvents = deduplicateService.getUniqDeviceEvents(deviceEvents);
        final var uniqDevice = deviceEventMapper.toDeviceIdList(uniqEvents);

        final var actual = uniqDevice.stream()
                .filter(Objects::nonNull)
                .filter(it -> StringUtils.isNotEmpty(it.getDeviceId()))
                .toList();

        deviceIdProducerImpl.send(actual);
    }

}
