package ts.andrey.eventservice.utils;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryPageTokenCache {

    private final ConcurrentMap<String, ConcurrentMap<Integer, String>> cache = new ConcurrentHashMap<>();

    private static final long TTL_MS = 10 * 60 * 1000;
    private final ConcurrentMap<String, Long> cacheCreationTime = new ConcurrentHashMap<>();

    public String getToken(String queryKey, int page) {
        cleanExpiredEntries();

        ConcurrentMap<Integer, String> pageTokens = cache.get(queryKey);
        if (pageTokens == null) {
            return null;
        }
        if (page == 0) {
            return null;
        }
        return pageTokens.get(page - 1);
    }

    public void saveToken(String queryKey, int currentPage, String token) {
        cleanExpiredEntries();

        if (!StringUtils.hasText(token)) {
            return;
        }
        ConcurrentMap<Integer, String> pageTokens = cache.computeIfAbsent(
                queryKey,
                k -> new ConcurrentHashMap<>()
        );

        pageTokens.put(currentPage, token);
        cacheCreationTime.putIfAbsent(queryKey, System.currentTimeMillis());
    }

    public void invalidate(String queryKey) {
        cache.remove(queryKey);
        cacheCreationTime.remove(queryKey);
    }

    private void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        cacheCreationTime.entrySet().removeIf(entry ->
                now - entry.getValue() > TTL_MS
        );

        // Удаляем соответствующие ключи из основного кэша
        cache.keySet().removeIf(key -> !cacheCreationTime.containsKey(key));
    }

    public String generateQueryKey(EventFilterRequest filter) {
        return String.format("%s:%d:%d:%s",
                filter.getDeviceId(),
                filter.getFromTimestamp() != null ? filter.getFromTimestamp() : 0,
                filter.getToTimestamp() != null ? filter.getToTimestamp() : 0,
                filter.getType() != null ? filter.getType() : ""
        );
    }
}
