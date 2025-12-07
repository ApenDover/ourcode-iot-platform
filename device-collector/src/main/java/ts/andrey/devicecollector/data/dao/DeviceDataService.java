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
import ts.andrey.devicecollector.metrics.DeviceCollectorMetrics;
import ts.andrey.devicecollector.metrics.PostgresMetrics;
import ts.andrey.devicecollector.utils.ShardUtil;
import ts.andrey.iotcommon.service.KafkaProducer;
import ts.andrey.iotcommon.utils.MessageDltBuilder;

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
    private final DeviceCollectorMetrics globalKafkaMetrics;
    private final KafkaProducer kafkaDltProducerImpl;
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
                .forEach(part -> {
                    try {
                        deviceBatchRepository.batchUpsert(part);
                    } catch (Exception e) {
                        globalKafkaMetrics.incrementError();
                        log.error("Ошибка при обработке батча:", e);
                        throw e;
                    }
                });
    }

    @Recover
    public void recover(DataAccessException e, List<Device> devices) {
        log.error("После нескольких попыток не удалось сохранить устройства в postgres, отправляем в DLT", e);
        final var errorMessages = devices.stream()
                .map(device -> {
                    final var shard = ShardUtil.getShardNameByString(device.getDeviceId(), shardCount);
                    postgresMetrics.incrementError(shard);
                    globalKafkaMetrics.incrementDltMessage();
                    return MessageDltBuilder.getMessage(device, e);
                }).toList();
        kafkaDltProducerImpl.send(errorMessages);
    }

}
