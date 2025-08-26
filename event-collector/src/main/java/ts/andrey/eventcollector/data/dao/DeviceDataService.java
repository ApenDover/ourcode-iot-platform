package ts.andrey.eventcollector.data.dao;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ts.andrey.eventcollector.data.entity.DeviceEntity;
import ts.andrey.eventcollector.data.repository.DeviceReactRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    private final DeviceReactRepository deviceReactRepository;

    @WithSpan("cassandra-save-batch-devices")
    public void saveDeviceIds(List<DeviceEntity> devices) {
        if (CollectionUtils.isEmpty(devices)) {
            return;
        }
        final var fluxEvents = Flux.fromIterable(devices);
        deviceReactRepository.saveAll(fluxEvents)
                .then()
                .doOnTerminate(() -> log.info("Сохранил Device в Cassandra: [{}]", devices.size()))
                .subscribe();
    }

    @WithSpan("cassandra-searching")
    public List<DeviceEntity> getUnsavedDeviceIds(List<DeviceEntity> devices) {
        final var ids = devices.stream()
                .map(DeviceEntity::getDeviceId)
                .distinct()
                .toList();

        log.info("Пытаюсь определить существование deviceId в cassandra [{}]", ids.size());

        final var modified = new ArrayList<>(devices);

        return deviceReactRepository.findAllById(Flux.fromIterable(ids))
                .collectList()
                .map(it -> {
                    modified.removeAll(it);
                    return modified;
                }).block();
    }

}
