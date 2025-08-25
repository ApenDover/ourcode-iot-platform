package ts.andrey.eventcollector.data.dao;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ts.andrey.eventcollector.data.entity.DeviceEventEntity;
import ts.andrey.eventcollector.data.repository.DeviceEventReactRepository;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.metrics.CassandraMetrics;
import ts.andrey.eventcollector.service.kafka.KafkaProducer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private final KafkaProducer kafkaEventProducerImpl;
    private final DeviceEventMapper deviceEventMapper;
    private final CassandraMetrics cassandraMetrics;
    private final DeviceEventReactRepository deviceEventReactRepository;

    @Value("${app.cassandra.batch-size}")
    private Integer bufferSize;

    @Value("${app.cassandra.concurrency-size}")
    private Integer concurrencySize;

    @Value("${app.cassandra.error.max-attempts}")
    private Integer maxAttempts;

    @Value("${app.cassandra.error.max-backoff}")
    private Integer maxBackoff;

    @Value("${app.cassandra.error.jitter-factor}")
    private Double jitterFactor;

    public Mono<Void> saveAll(List<DeviceEvent> events) {
        log.info("Сохраняю: [{}] событий в Cassandra", events.size());
        final var eventMap = events.stream()
                .collect(Collectors.toMap(
                        e -> UUID.fromString(e.getEventId()),
                        Function.identity()
                ));

        return Flux.fromIterable(deviceEventMapper.eventToEntityList(events))
                .buffer(bufferSize)
                .flatMap(batch -> saveBatch(batch, eventMap), concurrencySize)
                .doOnComplete(() -> log.info("Завершена обработка [{}] событий", events.size()))
                .then();
    }

    private Mono<Void> saveBatch(List<DeviceEventEntity> batch, Map<UUID, DeviceEvent> eventMap) {
        log.debug("Processing batch of [{}] events", batch.size());
        return deviceEventReactRepository.saveAll(batch)
                .doOnNext(e -> {
                    cassandraMetrics.incrementSuccess();
                    log.debug("Событие [{}] успешно сохранено", e.getKey().getEventId());
                })
                .then()
                .onErrorResume(e -> {
                    log.warn("Ошибка сохранения батча из [{}] событий, обработка ошибки", batch.size(), e);
                    return Flux.fromIterable(batch)
                            .flatMap(event -> saveSingle(event, eventMap))
                            .then();
                });
    }

    private Mono<Void> saveSingle(DeviceEventEntity event, Map<UUID, DeviceEvent> eventMap) {
        log.info("Обработка ошибки для [{}]", event);
        return deviceEventReactRepository.save(event)
                .doOnSuccess(e -> {
                    cassandraMetrics.incrementSuccess();
                    log.debug("Событие [{}] успешно сохранено", e.getKey().getEventId());
                })
                .retryWhen(
                        Retry.backoff(maxAttempts, Duration.ofSeconds(1))
                                .maxBackoff(Duration.ofSeconds(maxBackoff))
                                .jitter(jitterFactor)
                                .doBeforeRetry(retrySignal ->
                                        log.warn("Повторная попытка сохранения события [{}], попытка {}/[{}]",
                                                event.getKey().getEventId(),
                                                retrySignal.totalRetries() + 1,
                                                maxAttempts))
                )
                .onErrorResume(inner -> {
                    cassandraMetrics.incrementError();
                    log.error("Ошибка сохранения события [{}] в Cassandra после всех попыток",
                            event.getKey().getEventId(), inner);

                    Optional.ofNullable(eventMap.get(event.getKey().getEventId()))
                            .ifPresent(original -> kafkaEventProducerImpl.sendDlt(List.of(original)));

                    return Mono.empty();
                })
                .then();
    }

}
