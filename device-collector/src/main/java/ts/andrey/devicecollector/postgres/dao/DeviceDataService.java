package ts.andrey.devicecollector.postgres.dao;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.devicecollector.mapper.DeviceMapper;
import ts.andrey.devicecollector.postgres.entity.DeviceEntity;
import ts.andrey.devicecollector.postgres.repository.DeviceBatchRepository;
import ts.andrey.devicecollector.postgres.repository.DeviceRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;
    private final DeviceBatchRepository deviceBatchRepository;

    public List<DeviceEntity> saveAll(List<Device> devices) {
        final var deviceEntities = deviceMapper.toDeviceEntityList(devices);
        return deviceRepository.saveAll(deviceEntities);
    }

    public void batchUpsert(List<Device> devices) {
        final var deviceEntities = deviceMapper.toDeviceEntityList(devices);
        deviceBatchRepository.batchUpsert(deviceEntities);
    }

}
