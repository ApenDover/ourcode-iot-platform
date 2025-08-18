package ts.andrey.devicecollector.postgres.dao;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.devicecollector.annotation.WithSpan;
import ts.andrey.devicecollector.mapper.DeviceMapper;
import ts.andrey.devicecollector.postgres.repository.DeviceBatchRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    private final DeviceMapper deviceMapper;
    private final DeviceBatchRepository deviceBatchRepository;

    @WithSpan("postgres-save")
    public void batchUpsert(List<Device> devices) {
        final var deviceEntities = deviceMapper.toDeviceEntityList(devices);
        deviceBatchRepository.batchUpsert(deviceEntities);
    }

}
