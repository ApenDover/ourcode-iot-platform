package ts.andrey.eventcollector.cassandra.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ts.andrey.eventcollector.cassandra.entity.DeviceEntity;
import ts.andrey.eventcollector.cassandra.repository.DeviceReactRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    private final DeviceReactRepository deviceReactRepository;

    public void saveDeviceIds(List<DeviceEntity> devices) {
        log.info("Пытаюсь сохранить Device в cassandra {} записей", devices.size());
        final var fluxEvents = Flux.fromIterable(devices);
        deviceReactRepository.saveAll(fluxEvents)
                .then()
                .doOnTerminate(() -> log.info("Сохранил Device в Cassandra: {}", devices.size()))
                .subscribe();
    }

    public List<DeviceEntity> getUnsavedDeviceIds(List<DeviceEntity> devices) {
        log.info("Пытаюсь определить существование deviceId в cassandra {}", devices.size());

        final var ids = devices.stream()
                .map(DeviceEntity::getDeviceId)
                .distinct()
                .toList();

        final var modified = new ArrayList<>(devices);

        return deviceReactRepository.findAllById(Flux.fromIterable(ids))
                .collectList()
                .map(it -> {
                    modified.removeAll(it);
                    return modified;
                }).block();
    }

}
