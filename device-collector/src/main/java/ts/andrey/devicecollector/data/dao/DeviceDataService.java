package ts.andrey.devicecollector.data.dao;

import com.google.common.collect.Lists;
import com.nashkod.avro.Device;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import ts.andrey.devicecollector.data.repository.DeviceBatchRepository;
import ts.andrey.devicecollector.mapper.DeviceMapper;
import ts.andrey.devicecollector.metrics.GlobalMetrics;
import ts.andrey.devicecollector.metrics.PostgresMetrics;
import ts.andrey.devicecollector.service.kafka.KafkaProducer;
import ts.andrey.devicecollector.utils.ShardUtil;

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

    private final DeviceMapper deviceMapper;
    private final PostgresMetrics postgresMetrics;
    private final GlobalMetrics globalKafkaMetrics;
    private final KafkaProducer kafkaProducer;
    private final DeviceBatchRepository deviceBatchRepository;

    @WithSpan
    @Retryable(
            retryFor = DataAccessException.class,
            maxAttemptsExpression = "${app.postgres.retry.max-attempts:2}",
            backoff = @Backoff(
                    delayExpression = "${app.postgres.retry.initial-delay:1000}",
                    multiplierExpression = "${app.postgres.retry.multiplier:5}",
                    random = true
            )
    )
    public void batchUpsert(List<Device> devices) {
        final var deviceEntities = deviceMapper.toDeviceEntityList(devices);
        deviceEntities.forEach(e -> e.setId(UUID.randomUUID()));
        Lists.partition(deviceEntities, batchSize)
                .forEach(deviceBatchRepository::batchUpsert);
    }

    @Recover
    public void recover(DataAccessException e, List<Device> devices) {
        log.error("После нескольких попыток не удалось сохранить устройства в postgres, отправляем в DLT", e);
        devices.forEach(device -> {
            final var shard = ShardUtil.getShardNameByString(device.getDeviceId(), shardCount);
            postgresMetrics.incrementError(shard);
            globalKafkaMetrics.incrementDltMessage();
        });
        try {
            kafkaProducer.sendDlt(devices);
        } catch (Exception kafkaEx) {
            log.error("Ошибка отправки в DLT devices [{}]", devices, kafkaEx);
        }
    }

}
