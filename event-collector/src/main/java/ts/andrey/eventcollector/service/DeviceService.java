package ts.andrey.eventcollector.service;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.cassandra.dataService.DeviceEventDataService;
import ts.andrey.eventcollector.service.component.SimpleCache;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceEventDataService deviceEventDataService;
    private final DeviceEventProducer deviceIdProducerImpl;
    private final SimpleCache simpleCache;

    /***
     * Проверяем есть ли такие deviceId в cassandra,
     * если нет - отправляем в kafka топик,
     * кешируем
     *
     * @param devices - список устройств
     * @return - список новых устройств, которые обработали
     */
    public List<Device> process(List<Device> devices) {
        if (CollectionUtils.isEmpty(devices)) {
            return List.of();
        }
        final var unsavedDevices = devices.stream()
                .filter(deviceEvent -> !deviceEventDataService.isExistDeviceId(deviceEvent.getDeviceId()))
                .toList();

        if (!CollectionUtils.isEmpty(unsavedDevices)) {
            deviceIdProducerImpl.send(unsavedDevices);
            final var deviceIds = unsavedDevices.stream()
                    .map(Device::getDeviceId)
                    .toList();
            simpleCache.putAll(deviceIds);
        }
        return unsavedDevices;
    }

}
