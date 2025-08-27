package ts.andrey.deviceservice.data.dao;

import com.google.common.collect.Lists;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import ts.andrey.device.model.Device;
import ts.andrey.deviceservice.data.repository.DeviceBatchRepository;
import ts.andrey.deviceservice.utils.ShardUtil;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    @Value("${app.shardingSphere.shardCount}")
    private Integer shardCount;

    @Value("${app.postgres.batch-size:300}")
    private int batchSize;

    private final DeviceBatchRepository deviceBatchRepository;

    @WithSpan
    public void batchUpsert(List<Device> devices) {
//        final var deviceEntities = deviceMapper.toDeviceEntityList(devices);
//        deviceEntities.forEach(e -> e.setId(UUID.randomUUID()));
//        Lists.partition(deviceEntities, batchSize)
//                .forEach(deviceBatchRepository::batchUpsert);
    }


}
