package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.cassandra.dao.DeviceEventDataService;
import ts.andrey.eventcollector.service.DeduplicateService;
import ts.andrey.eventcollector.service.component.SimpleCache;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicateServiceImpl implements DeduplicateService {

    private final DeviceEventDataService deviceEventDataService;
    private final SimpleCache simpleCache;

    /**
     * @return список device которых нет ни в simpleCache ни в cassandra
     */
    public List<Device> getUniqueDevices(List<Device> devices) {

        final var uncached = devices.stream()
                .filter(Objects::nonNull)
                .filter(deviceEvent -> StringUtils.isNotEmpty(deviceEvent.getDeviceId()))
                .distinct()
                .filter(it -> !simpleCache.contains(it.getDeviceId()))
                .toList();

        final var ids = uncached.stream()
                .map(Device::getDeviceId)
                .distinct()
                .toList();


        final var unsavedDeviceIds = deviceEventDataService.getUnsavedDeviceIds(ids);

        return uncached.stream()
                .filter(Objects::nonNull)
                .filter(device -> StringUtils.isNotEmpty(device.getDeviceId())
                        && unsavedDeviceIds.contains(device.getDeviceId()))
                .toList();
    }

}
