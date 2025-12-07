package ts.andrey.devicecollector.service.impl;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.devicecollector.data.dao.DeviceDataService;
import ts.andrey.devicecollector.metrics.DeviceCollectorMetrics;
import ts.andrey.devicecollector.service.DeviceService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceDataService deviceDataService;
    private final DeviceCollectorMetrics globalKafkaMetrics;

    @Override
    public void createOrUpdateDevice(List<Device> devices) {
        deviceDataService.batchUpsert(devices);
        log.info("Сохранены или обновлены девайсы [{}]", devices.size());
        globalKafkaMetrics.incrementSuccess();
    }

}
