package ts.andrey.eventcollector.cassandra.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.repository.DeviceEventReactRepository;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private final DeviceEventReactRepository deviceEventReactRepository;

    public void saveAll(List<DeviceEventEntity> events) {
        log.info("Attempting to save {} DeviceEvent records to Cassandra", events.size());

        Flux.fromIterable(events)
                .buffer(1000)
                .flatMap(batch -> {
                    log.debug("Processing batch of {} events", batch.size());
                    return deviceEventReactRepository.saveAll(batch)
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                            .onErrorResume(e -> {
                                log.error("Failed to save batch of {} events "
                                        + "to Cassandra - skipping batch", batch.size(), e);
                                return Mono.empty();
                            });
                })
                .doOnComplete(() -> log.info("Successfully processed all {} events", events.size()))
                .doOnError(e -> log.error("Error processing events", e))
                .blockLast();
    }

}
