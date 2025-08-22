package ts.andrey.eventcollector.cassandra.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.repository.DeviceEventReactRepository;
import ts.andrey.eventcollector.metrics.CassandraMetrics;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private final CassandraMetrics cassandraMetrics;
    private final DeviceEventReactRepository deviceEventReactRepository;

    @Value("${app.cassandra.batch-size}")
    private Integer bufferSize;

    @Value("${app.cassandra.concurrency-size}")
    private Integer concurrencySize;

    public void saveAll(List<DeviceEventEntity> events) {
        log.info("Сохраняю: [{}] событий в Cassandra", events.size());

        Flux.fromIterable(events)
                .buffer(bufferSize)
                .flatMap(batch -> {
                    log.debug("Processing batch of {} events", batch.size());
                    return deviceEventReactRepository.saveAll(batch)
                            .collectList()
                            .doOnSuccess(saved -> {
                                cassandraMetrics.incrementSuccess(saved.size());
                                log.debug("Успешно сохранен батч из {} событий", batch.size());
                            })
                            .onErrorResume(e -> {
                                log.warn("Ошибка сохранения батча из {} событий, пробуем поштучно", batch.size(), e);

                                return Flux.fromIterable(batch)
                                        .flatMap(event ->
                                                deviceEventReactRepository.save(event)
                                                        .doOnSuccess(saved -> {
                                                            cassandraMetrics.incrementSuccess();
                                                            log.debug("Событие {} успешно сохранено", event.getKey().getEventId());
                                                        })
                                                        .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                                                        .onErrorResume(inner -> {
                                                            cassandraMetrics.incrementError();
                                                            log.error("Ошибка сохранения события {} в Cassandra", event.getKey().getEventId(), inner);
                                                            return Mono.empty();
                                                        })
                                        )
                                        .collectList();
                            });
                }, concurrencySize)
                .doOnComplete(() -> log.info("Завершена обработка {} событий", events.size()))
                .blockLast();
    }

}
