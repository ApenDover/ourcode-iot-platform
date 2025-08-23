package ts.andrey.devicecollector.postgres.dao;

import com.google.common.collect.Lists;
import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ts.andrey.devicecollector.annotation.WithSpan;
import ts.andrey.devicecollector.mapper.DeviceMapper;
import ts.andrey.devicecollector.postgres.repository.DeviceBatchRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    @Value("${app.postgres.batch-size}")
    private Integer batchSize;

    private final DeviceMapper deviceMapper;
    private final DeviceBatchRepository deviceBatchRepository;

    @WithSpan("postgres-save")
    public void batchUpsert(List<Device> devices) {
        final var deviceEntities = deviceMapper.toDeviceEntityList(devices);
        deviceEntities.forEach(e -> e.setId(UUID.randomUUID()));

        Lists.partition(deviceEntities, batchSize)
                .forEach(deviceBatchRepository::batchUpsert);
    }

}
