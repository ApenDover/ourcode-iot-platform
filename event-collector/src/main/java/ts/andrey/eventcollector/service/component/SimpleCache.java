package ts.andrey.eventcollector.service.component;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SimpleCache {

    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();
    private final long expirationMillis;
    private final long expirationSize;

    public SimpleCache(@Value("${app.cache.expiration.seconds:30}") long expirationSeconds,
                       @Value("${app.cache.expiration.size:900}") long expirationSize) {
        this.expirationMillis = TimeUnit.SECONDS.toMillis(expirationSeconds);
        this.expirationSize = expirationSize;
    }

    public void put(String deviceId) {
        if (Objects.isNull(deviceId)) {
            return;
        }
        cache.put(deviceId, System.currentTimeMillis());
        log.debug("deviceId={} добавлен в кеш, объектов в кеше={}", deviceId, cache.size());
        if (expirationSize <= size()) {
            cleanUp();
        }
    }

    public boolean contains(String deviceId) {
        if (StringUtils.isEmpty(deviceId)) {
            return false;
        }
        cache.computeIfPresent(deviceId, (k, oldValue) -> System.currentTimeMillis());
        return cache.containsKey(deviceId);
    }

    public void remove(String deviceId) {
        if (Objects.isNull(deviceId)) {
            return;
        }
        cache.remove(deviceId);
    }

    public void cleanUp() {
        final var size = cache.size();
        cache.entrySet().removeIf(entry ->
                entry.getKey() == null
                        || entry.getValue() == null
                        || isExpired(entry.getValue())
        );
        log.debug("Кеш очищен, {} -> {}", size, cache.size());
    }

    private boolean isExpired(long timestamp) {
        return (System.currentTimeMillis() - timestamp) > expirationMillis;
    }

    public int size() {
        return cache.size();
    }

    public Map<String, Long> getCache() {
        return new HashMap<>(cache);
    }

}
