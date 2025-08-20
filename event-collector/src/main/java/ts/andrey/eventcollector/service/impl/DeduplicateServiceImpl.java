package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.annotation.WithSpan;
import ts.andrey.eventcollector.cassandra.dao.DeviceDataService;
import ts.andrey.eventcollector.cassandra.dao.DeviceEventDataService;
import ts.andrey.eventcollector.cassandra.entity.DeviceEntity;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.DeduplicateService;
import ts.andrey.eventcollector.service.component.SimpleCache;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicateServiceImpl implements DeduplicateService {

    private final DeviceEventDataService deviceEventDataService;
    private final DeviceDataService deviceDataService;
    private final SimpleCache simpleCache;
    private final DeviceEventMapper deviceEventMapper;

    /**
     * @return список device которых нет ни в simpleCache ни в cassandra
     */
    @WithSpan("deduplicateService")
    public List<Device> getUniqueDevices(List<Device> devices) {

        final var uncached = devices.stream()
                .filter(Objects::nonNull)
                .filter(deviceEvent -> deviceEvent.getDeviceId() > 0)
                .distinct()
                .filter(it -> !simpleCache.contains(String.valueOf(it.getDeviceId())))
                .toList();

        final var deviceEntities = deviceEventMapper.deviceToEntityList(uncached);
        final var unsavedDevices = deviceDataService.getUnsavedDeviceIds(deviceEntities);
        deviceDataService.saveDeviceIds(unsavedDevices);

        final var unsavedDeviceIds = unsavedDevices.stream()
                .map(DeviceEntity::getDeviceId)
                .distinct()
                .toList();

        return uncached.stream()
                .filter(Objects::nonNull)
                .filter(device -> device.getDeviceId() > 0
                        && unsavedDeviceIds.contains(String.valueOf(device.getDeviceId())))
                .filter(distinctByKey(Device::getDeviceId))
                .toList();
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}
