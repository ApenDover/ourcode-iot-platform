package ts.andrey.devicecollector.postgres.dao;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.devicecollector.annotation.WithSpan;
import ts.andrey.devicecollector.mapper.DeviceMapper;
import ts.andrey.devicecollector.postgres.repository.DeviceBatchRepository;
import ts.andrey.devicecollector.postgres.repository.DeviceRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    private final DeviceMapper deviceMapper;
    private final DeviceBatchRepository deviceBatchRepository;
    private final DeviceRepository deviceRepository;

    @WithSpan("postgres-save")
    public void batchUpsert(List<Device> devices) {
        final var deviceEntities = deviceMapper.toDeviceEntityList(devices);
        deviceEntities.forEach(e -> e.setId(UUID.randomUUID()));
//        deviceRepository.saveAll(deviceEntities);
        deviceBatchRepository.batchUpsert(deviceEntities);
    }

}
